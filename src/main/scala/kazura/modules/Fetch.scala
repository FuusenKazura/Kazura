package kazura.modules

import chisel3._
import chisel3.util.MuxCase
import kazura.models.Inst
import kazura.util.Params._

class IFIO extends Bundle {
  val in: IFIn = Input(new IFIn)
  val out: IFOut = Output(new IFOut)
}
class IFIn extends Bundle {
  val is_branch_or_jump_detected: Bool = Bool()
  val alu_out: UInt = UInt(LEN.W)
  val stole: Bool = Bool()
}
class IFOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
  val inst: Inst = new Inst
}

class Fetch(init: Seq[UInt] = (10 until 266).map(_.U)) extends Module  {
  val io: IFIO = IO(new IFIO)
  val total_cnt: UInt = RegInit(0.U(LEN.W))
  val pc: UInt = RegInit(0.U(LEN.W))
  val inst_mem: Vec[UInt] = RegInit(VecInit(init))
  total_cnt := total_cnt + 1.U
  pc := pc + Mux(io.in.is_branch_or_jump_detected, io.in.alu_out, Mux(io.in.stole, 0.U, 1.U))
  io.out.pc := pc
  io.out.total_cnt := total_cnt
  io.out.inst := inst_mem(pc).asTypeOf(new Inst)
}

