package kazura.modules

import chisel3._
import chisel3.util._
import kazura.util.Params._

class ROBIO extends Bundle {
  val used_num: UInt = Input(UInt(log2Ceil(PARALLEL + 1).W)) // 0 to PARALLEL, 先のクロックで、いくつROBが必要とされたか
  val graduate: Vec[Valid[ROBGraduate]] = Input(Vec(PARALLEL*2, Valid(new ROBGraduate)))
  val commit: Vec[Valid[ROBEntry]] = Output(Vec(PARALLEL, Valid(new ROBEntry)))

  val available_head: Valid[UInt] = Output(Valid(UInt()))
}

class ROBEntry extends Bundle with ROBEntryT
trait ROBEntryT extends ROBGraduateT {
  val empty: Bool = Bool()
  val committable: Bool = Bool()
}

class ROBGraduate extends Bundle with ROBGraduateT {
  val addr: UInt = UInt(log2Ceil(ROB.BUF_SIZE).W)
}
trait ROBGraduateT {
  val alu_out: UInt = UInt(LEN.W)
  val rd: UInt = UInt(log2Ceil(RF.NUM).W)
  val is_halt: Bool = Bool()
  val is_jump: Bool = Bool()
  val is_branch: Bool = Bool()
  val reg_w: Bool = Bool()
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
}

class ROB extends Module {
  val io: ROBIO = IO(new ROBIO)

  val buf_init: ROBEntry = Wire(new ROBEntry)
  buf_init.empty := true.B

  val buf: Vec[ROBEntry] = RegInit(VecInit(Seq.fill(ROB.BUF_SIZE)(buf_init)))
  val uncommited_head: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))
  val unreserved_head: UInt = RegInit(0.U(log2Ceil(ROB.BUF_SIZE).W))

  // ROBがreserveされた分だけ進める(不可能なら進めない)
  val next_unreserved_head: UInt = unreserved_head + io.used_num
  val next_unreserved_head_available: Bool = buf(next_unreserved_head).empty
  io.available_head.valid := next_unreserved_head_available
  io.available_head.bits := next_unreserved_head
  unreserved_head := Mux(next_unreserved_head_available, next_unreserved_head, unreserved_head)

  // このROB Entryをcommitするかどうか
  val committable: Vec[Bool] = Wire(Vec(PARALLEL, Bool()))
  for (i <- committable.indices) {
    val addr: UInt = uncommited_head + i.U
    if (i == 0) {
      committable(i) := buf(addr).committable
    } else {
      committable(i) := committable(i-1) && buf(addr).committable
    }
  }
  val committable_cnt: UInt = committable.count(x => x)
  uncommited_head := uncommited_head + committable_cnt

  // commit
  for (i <- io.commit.indices) {
    io.commit(i).valid := false.B
  }
  for (i <- buf.indices) {
    when(uncommited_head <= i.U && i.U < uncommited_head+committable_cnt) {
      buf(i).empty := true.B
      val l = i.U - uncommited_head
      io.commit(l).valid := true.B
      io.commit(l).bits := buf(i)
    } .elsewhen(unreserved_head <= i.U && i.U < unreserved_head+io.used_num) {
      buf(i).empty := false.B
    } .otherwise {
      val graduate = io.graduate.reduceTree((g1: Valid[ROBGraduate], g2: Valid[ROBGraduate]) =>
        Mux(g1.valid && g1.bits.addr === i.U, g1, g2))

      when (graduate.valid && i.U === graduate.bits.addr) {
        buf(i).committable := true.B
        buf(i).is_halt := graduate.bits.is_halt
        buf(i).is_jump := graduate.bits.is_jump
        buf(i).is_branch := graduate.bits.is_branch
        buf(i).reg_w := graduate.bits.reg_w
        buf(i).mem_r := graduate.bits.mem_r
        buf(i).mem_w := graduate.bits.mem_w
        buf(i).rd := graduate.bits.rd
        buf(i).alu_out := graduate.bits.alu_out
      }
    }
  }
}
