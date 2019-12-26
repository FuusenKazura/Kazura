package kazura.modules

import chisel3._
import chisel3.util.MuxCase
import kazura.models.InstBits
import kazura.util.Params._

class IFIO extends Bundle {
  val in: IFIn = Input(new IFIn)
  val out: IFOut = Output(new IFOut)
}
class IFIn extends Bundle {
  val is_branch: Bool = Bool()
  val is_jump: Bool = Bool()
  val alu_out: UInt = UInt(LEN.W)
  val stall: Bool = Bool()
}
class IFOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
  val inst: InstBits = new InstBits
}

class Fetch(init: Seq[UInt] = (10 until 266).map(_.U)) extends Module  {
  val io: IFIO = IO(new IFIO)
  val total_cnt: UInt = RegInit(0.U(LEN.W))
  val pc: UInt = RegInit(0.U(LEN.W))
  val inst_mem: Vec[UInt] = RegInit(VecInit(init))
  total_cnt := total_cnt + 1.U
  pc := MuxCase(pc+1.U, Seq(
    io.in.is_jump -> io.in.alu_out,
    io.in.is_branch -> io.in.alu_out,
    io.in.stall -> pc))
  io.out.pc := pc
  io.out.total_cnt := total_cnt
  io.out.inst := inst_mem(pc).asTypeOf(new InstBits)
}

