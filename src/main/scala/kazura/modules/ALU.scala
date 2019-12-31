package kazura.modules

import chisel3._
import chisel3.util.MuxLookup
import kazura.util.Params._

class ALUIO extends Bundle {
  val ctrl: Ctrl = Input(new Ctrl)
  val source: Vec[UInt] = Vec(2, Input(UInt(LEN.W)))
  val source_enable: Bool = Input(Bool())

  val alu_out: UInt = Output(UInt(LEN.W))
  val alu_ctrl_out: Ctrl = Output(new Ctrl)
}

class ALU extends Module {
  val io: ALUIO = IO(new ALUIO)
  io.alu_out := RegNext(MuxLookup(io.ctrl.alu_op, 0.U, Seq(
    ALUOP.ADD.U -> io.source(0).+(io.source(1)),
    ALUOP.SUB.U -> io.source(0).-(io.source(1)),
    ALUOP.AND.U -> io.source(0).&(io.source(1)),
    ALUOP.OR.U  -> io.source(0).|(io.source(1)),
    ALUOP.EQ.U  -> io.source(0).===(io.source(1)),
    ALUOP.GT.U  -> io.source(0).>(io.source(1))
  )))
  io.alu_ctrl_out := RegNext(io.ctrl, 0.U.asTypeOf(new Ctrl))
}
