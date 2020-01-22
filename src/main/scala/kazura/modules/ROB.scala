package kazura.modules

import chisel3._
import chisel3.util._
import kazura.models.{Inst, InstInfo}
import kazura.util.Params._

class ROBIO extends Bundle {
  val used_num: UInt = Input(UInt(log2Ceil(PARALLEL + 1).W)) // 0 to PARALLEL, 先のクロックで、いくつROBが必要とされたか
  val graduate: Vec[Valid[ROBGraduate]] = Input(Vec(PARALLEL*2, Valid(new ROBGraduate)))
  val commit: Vec[RFWrite] = Output(Vec(PARALLEL, new RFWrite))
  val unreserved_head: Valid[UInt] = Output(Valid(UInt(log2Ceil(ROB.BUF_SIZE).W)))
}

class ROBEntry extends Bundle with ROBEntryT
trait ROBEntryT extends ROBGraduateT {
  val empty: Bool = Bool()
  val committable: Bool = Bool()
}

class ROBGraduate extends Bundle with ROBGraduateT {
  val addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W)
  val mispredicted: Bool = Bool()
}
trait ROBGraduateT {
  val data: UInt = UInt(LEN.W)
  val ctrl: InstInfo = new InstInfo
}

class ROB extends Module {
  val io: ROBIO = IO(new ROBIO)

  val buf_init: ROBEntry = Wire(new ROBEntry)
  buf_init.empty := true.B

  buf_init.committable := false.B

  buf_init.data := 0.U
  buf_init.ctrl := InstInfo.nop
  // buf_init.rd_addr := 0.U
  // buf_init.is_halt := false.B
  // buf_init.reg_w := false.B
  // buf_init.mem_r := false.B
  // buf_init.mem_w := false.B

  val buf: Vec[ROBEntry] = RegInit(VecInit(Seq.fill(ROB.BUF_SIZE)(buf_init)))
  val uncommited_head_r: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))
  val unreserved_head_r: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))

  // graduatesの中ではじめに失敗している命令を抽出
  // TODO: 複数分岐命令が同時にgraduateした際にバグる可能性が高い
  //  リングバッファ上のアドレスのみで、古いほうの分岐命令をselectするのは無理なので、なにか仕組みを導入する必要がある
  val first_mispredicted: Valid[ROBGraduate] = io.graduate.reduceTree(
    (g1: Valid[ROBGraduate], g2: Valid[ROBGraduate]) => Mux(g1.valid && g1.bits.mispredicted, g1, g2))
  val mispredicted_addr: Valid[UInt] = Wire(Valid(UInt()))
  mispredicted_addr.valid :=
    first_mispredicted.valid && first_mispredicted.bits.mispredicted && !buf(first_mispredicted.bits.addr).empty
  mispredicted_addr.bits := first_mispredicted.bits.addr
  printf("mispredicted_rob: %d | %d\n", mispredicted_addr.valid, mispredicted_addr.bits)

  val unreserved_head: Valid[UInt] = Wire(Valid(UInt(log2Ceil(ROB.BUF_SIZE).W)))
  // ROBがreserveされた分だけ進める(不可能なら進めない)
  val next_unreserved_head: UInt = unreserved_head_r + io.used_num
  val next_unreserved_head_available: Bool = buf(next_unreserved_head).empty
  printf("next_rob: %d | %d\n", next_unreserved_head_available, next_unreserved_head)

  when (mispredicted_addr.valid) {
    // 分岐予測失敗時、分岐命令のROB Addrまで巻き戻す
    unreserved_head.valid := mispredicted_addr.valid
    unreserved_head.bits := mispredicted_addr.bits
  } .otherwise {
    unreserved_head.valid := next_unreserved_head_available
    unreserved_head.bits := next_unreserved_head
  }
  printf("rob: %d | %d\n", unreserved_head.valid, unreserved_head.bits)

  unreserved_head_r := Mux(unreserved_head.valid, unreserved_head.bits, unreserved_head_r)
  io.unreserved_head.valid := RegNext(unreserved_head.valid, true.B) // 初期はすべてのROBが確保可能
  io.unreserved_head.bits := unreserved_head_r

  // このROB Entryをcommitするかどうか
  val committable: Vec[Bool] = Wire(Vec(PARALLEL, Bool()))
  for (i <- committable.indices) {
    val addr: UInt = uncommited_head_r + i.U
    if (i == 0) {
      committable(i) := buf(addr).committable
    } else {
      committable(i) := committable(i-1) && buf(addr).committable
    }
  }
  val committable_cnt: UInt = committable.count(x => x)
  uncommited_head_r := uncommited_head_r + committable_cnt
  printf(s"commmitable_cnt: %d\n", committable_cnt)

  // commit初期値
  for (i <- io.commit.indices) {
    io.commit(i).rf_w := false.B; io.commit(i).rd_addr := 0.U; io.commit(i).data := 0.U
  }
  for (i <- buf.indices) {
    // mispredictedのロジックがバグってる
    when (mispredicted_addr.valid && uncommited_head_r < unreserved_head_r && (
      mispredicted_addr.bits <= i.U && i.U < unreserved_head_r)) {
      // mispredicted時、分岐に依存した命令は取り消す必要がある(不要なロジックも可能なので、そのようなものも考えても良い)

      // mispredicted_addr <= i < uncommittedの命令が取り消す対象になるが
      // bufはリングバッファなので(i%buf.sizeなので)位置関係によって変わってくる

      // uncommitted, mispredicted, 取り消す対象, unreservedの並びの時
      buf(i).empty := true.B
    } .elsewhen(mispredicted_addr.valid && uncommited_head_r > unreserved_head_r && (
      i.U < unreserved_head_r || mispredicted_addr.bits <= i.U )) {
        // 取り消す対象, unreserved, uncommitted, mispredicted, 取り消す対象の並びの時
        buf(i).empty := true.B
    } .elsewhen(buf(i).empty && unreserved_head_r <= i.U && i.U < unreserved_head_r+io.used_num) {
      // dispatch
      buf(i).empty := false.B
      buf(i).committable := false.B
    } .elsewhen(uncommited_head_r <= i.U && i.U < uncommited_head_r+committable_cnt) {
      // commit
      buf(i).empty := true.B
      val l = i.U - uncommited_head_r
      printf(s"commit(%d).data = %d\n", l, buf(i).data)
      io.commit(l).rf_w := buf(i).ctrl.rf_w
      io.commit(l).rd_addr := buf(i).ctrl.rd_addr
      io.commit(l).data := buf(i).data
    } .otherwise {
      // graduate
      // EX, MAから出てくるデータを取り込む、それらのデータはDispatch, Commitの対象になることはないので問題なし

      val graduate = io.graduate.reduceTree((g1: Valid[ROBGraduate], g2: Valid[ROBGraduate]) =>
        Mux(g1.valid && g1.bits.addr === i.U, g1, g2))

      when (graduate.valid && i.U === graduate.bits.addr) {
        buf(i).committable := true.B
        buf(i).ctrl := graduate.bits.ctrl
        // buf(i).is_halt := graduate.bits.is_halt
        // buf(i).reg_w := graduate.bits.reg_w
        // buf(i).mem_r := graduate.bits.mem_r
        // buf(i).mem_w := graduate.bits.mem_w
        // buf(i).rd_addr := graduate.bits.rd_addr
        buf(i).data := graduate.bits.data
      }
    }
  }
  for (i <- 0 until 9) {
    printf("buf(%d): empty: %d | commitable: %d | rw: %d | data: %d\n", i.U,
      buf(i).empty, buf(i).committable, buf(i).ctrl.rf_w, buf(i).data)
  }
  printf("------------------------------\n")
}
