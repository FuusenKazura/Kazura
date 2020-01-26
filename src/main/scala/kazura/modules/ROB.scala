package kazura.modules

import chisel3._
import chisel3.util._
import kazura.models.{Ctrl, Inst, InstInfo}
import kazura.util.Params._

class ROBIO extends Bundle {
  val used_num: UInt = Input(UInt(log2Ceil(PARALLEL + 1).W)) // 0 to PARALLEL, 先のクロックで、いくつROBが必要とされたか
  val graduate: Vec[Valid[ROBGraduate]] = Input(Vec(PARALLEL*2, Valid(new ROBGraduate))) // 今はALUの個数+メモリユニットからの個数個のgraduateがありえる
  val commit: Vec[RFWrite] = Output(Vec(PARALLEL, new RFWrite))
  val unreserved_head: Vec[Valid[UInt]] = Output(Vec(PARALLEL, Valid(UInt(log2Ceil(ROB.BUF_SIZE).W))))
}

class ROBEntry extends Bundle with ROBEntryT
trait ROBEntryT extends ROBGraduateT {
  val reserved: Bool = Bool()
  val committable: Bool = Bool()
  val mispredicted: Bool = Bool()
}

class ROBGraduate extends Bundle with ROBGraduateT {
  val addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W) // TODO: InstInfoと纏める
  val mispredicted: Bool = Bool()
}
trait ROBGraduateT {
  val data: UInt = UInt(LEN.W)
  val inst_info: InstInfo = new InstInfo
}

class ROB extends Module {
  val io: ROBIO = IO(new ROBIO)

  val buf_init: ROBEntry = Wire(new ROBEntry)

  buf_init.reserved := false.B
  buf_init.committable := false.B
  buf_init.mispredicted := false.B

  buf_init.data := 0.U
  buf_init.inst_info := InstInfo.nop
  val buf: Vec[ROBEntry] = RegInit(VecInit(Seq.fill(ROB.BUF_SIZE)(buf_init)))

  val uncommited: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))
  val commitable: Vec[Bool] = WireDefault(VecInit(
    (0 until PARALLEL).map(i => buf(uncommited + i.U).committable)))
  val can_commit_cnt_v: Vec[Bool] = Wire(Vec(PARALLEL, Bool()))
  can_commit_cnt_v(0) := commitable(0)
  for (i <- 1 until PARALLEL) can_commit_cnt_v(i) := can_commit_cnt_v(i-1) && commitable(i)
  // いくつcommit出来るか求める
  val can_commit_cnt: UInt = can_commit_cnt_v.count((x: Bool) => x)
  val next_uncommited: UInt = uncommited+can_commit_cnt
  uncommited := next_uncommited

  val mispredicted_first: Valid[ROBGraduate] = io.graduate.reduceTree((g1: Valid[ROBGraduate], g2: Valid[ROBGraduate]) => Mux(g1.valid && g1.bits.mispredicted, g1, g2))
  val mispredicted: Bool = mispredicted_first.valid && mispredicted_first.bits.mispredicted
  val mispredict_rob_addr: UInt = mispredicted_first.bits.addr

  val unreserved: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))
  val unreserved_add_used: UInt = unreserved + io.used_num
  val unreserved_add_used_valid: Bool = !buf(unreserved_add_used).reserved
  val next_unreserved: UInt = unreserved_add_used
  unreserved := next_unreserved

  for (i <- buf.indices) {
    val graduate: Valid[ROBGraduate] = io.graduate.reduceTree((g1: Valid[ROBGraduate], g2: Valid[ROBGraduate]) =>
      Mux(g1.valid && g1.bits.addr === i.U, g1, g2))

    val mispredict_restore_entry: Bool = (mispredicted && (
        (mispredict_rob_addr <= next_unreserved && mispredict_rob_addr <= i.U && i.U < next_unreserved) ||
        (mispredict_rob_addr > next_unreserved && (i.U < next_unreserved || mispredict_rob_addr <= i.U))
      ))
    val reserve_entry = (
      ((unreserved <= next_unreserved) && (unreserved <= i.U && i.U < next_unreserved)) ||
      ((unreserved > next_unreserved) && (i.U < next_unreserved || unreserved <= i.U))
    )
    val commit_entry = (
      ((uncommited <= next_uncommited) && (uncommited <= i.U && i.U < next_uncommited)) ||
      ((uncommited >  next_uncommited) && (i.U < next_uncommited || uncommited <= i.U))
    )
    val store_entry = graduate.valid && graduate.bits.addr === i.U

    printf("buf(%d) | pc: %d, total_cnt: %d, mispredict: %d, reserve: %d, commit: %d, store: %d | reserved: %d, commitable: %d, data: %d, rf_w: %d\n",
      i.U, buf(i).inst_info.pc, buf(i).inst_info.total_cnt, mispredict_restore_entry, reserve_entry, commit_entry, store_entry, buf(i).reserved, buf(i). committable, buf(i).data, buf(i).inst_info.ctrl.rf_w)

    when(mispredict_restore_entry && store_entry) {
      // mispredictとstoreは同時に発生しうる
      buf(i).mispredicted := true.B
      buf(i).committable := true.B
      buf(i).data := graduate.bits.data
      buf(i).inst_info := graduate.bits.inst_info
    } .elsewhen(mispredict_restore_entry) {
      buf(i).mispredicted := true.B
    } .elsewhen(reserve_entry) {
      buf(i).reserved := true.B
      buf(i).committable := false.B
    }.elsewhen(store_entry) {
      buf(i).committable := true.B
      buf(i).data := graduate.bits.data
      buf(i).inst_info := graduate.bits.inst_info
    } .elsewhen(commit_entry) {
      buf(i).reserved := false.B
      buf(i).committable := false.B
      buf(i).mispredicted := false.B
    }
  }

  for (i <- 0 until PARALLEL) {
    val commit_entry = buf(uncommited + i.U)
    when (i.U < can_commit_cnt) {
      io.commit(i).rf_w := commit_entry.inst_info.ctrl.rf_w
      io.commit(i).rd_addr := commit_entry.inst_info.rd_addr
      io.commit(i).rob_addr := uncommited + i.U
      io.commit(i).mispredict := commit_entry.mispredicted
      io.commit(i).data := commit_entry.data
    } .otherwise {
      io.commit(i).rf_w := false.B
      io.commit(i).rd_addr := 0.U
      io.commit(i).rob_addr := 0.U
      io.commit(i).mispredict := false.B
      io.commit(i).data := 655535.U
    }
    for (i <- io.commit.indices) {
      printf("commit(%d): rfw: %d, rd: %d, rob: %d, mispredict: %d, data: %d\n",
        i.U,io.commit(i).rf_w,io.commit(i).rd_addr,io.commit(i).rob_addr,io.commit(i).mispredict,io.commit(i).data)
    }
  }
  for (i <- 0 until PARALLEL) {
    io.unreserved_head(i).valid := unreserved_add_used_valid
    io.unreserved_head(i).bits := unreserved + i.U
  }

  printf("unreserved: %d, next_unreserved: %d\n", unreserved, next_unreserved)
  printf("uncommited: %d, next_uncommited: %d, can_commit_cnt: %d\n", uncommited, next_uncommited, can_commit_cnt)
  printf("mispredicted: %d, unreserved_add_used_valid: %d\n", mispredicted, unreserved_add_used_valid)
  printf("-----------------------------------\n")
}
