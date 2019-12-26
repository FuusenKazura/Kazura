package kazura.modules

import chisel3._
import chisel3.util.MuxCase
import kazura.models.InstBits
import kazura.util.Params._

class FetchIO extends Bundle {
  val in: FetchIn = Input(new FetchIn)
  val out: FetchOut = Output(new FetchOut)
}
class FetchIn extends Bundle {
  val prev_pc: UInt = UInt()
  val prev_total_cnt: UInt = UInt()
  val is_branch: Bool = Bool()
  val is_jump: Bool = Bool()
  val alu_out: UInt = UInt(LEN.W)
  val stall: Bool = Bool()
}
class FetchOut extends Bundle {
  val pc: UInt = UInt(LEN.W)
  val total_cnt: UInt = UInt(LEN.W)
}

class Fetch extends Module  {
  val io: FetchIO = IO(new FetchIO)
  io.out.total_cnt := io.in.prev_total_cnt + 1.U
  io.out.pc := MuxCase(io.in.prev_pc+1.U, Seq(
    io.in.is_jump -> io.in.alu_out,
    io.in.is_branch -> io.in.alu_out,
    io.in.stall -> io.in.prev_pc))
}
