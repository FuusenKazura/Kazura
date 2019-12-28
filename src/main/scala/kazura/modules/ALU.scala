package kazura.modules

import chisel3._
import chisel3.util.MuxLookup
import kazura.util.Params._

class ALUIO extends Bundle {
  val ctrl: Ctrl = Input(new Ctrl)
  val source: Vec[UInt] = Vec(2, Input(UInt(LEN.W)))
  val source_enable: Bool = Input(Bool())

  val alu_out: UInt = Output(UInt(LEN.W))
  val alu_out_enable: Bool = Output(Bool())
  val alu_ctrl_out: Ctrl = Output(new Ctrl)
}

class ALU extends Module {
  val io: ALUIO = IO(new ALUIO)
  io.alu_out := RegNext(MuxLookup(0, io.ctrl.alu_op, Seq(
    ALUOP.ADD -> io.source(0).+(io.source(1)),
    ALUOP.SUB -> io.source(0).-(io.source(1)),
    ALUOP.AND -> io.source(0).*(io.source(1)),
    ALUOP.OR  -> io.source(0)./(io.source(1))
  )))
  io.alu_out_enable := RegNext(io.source_enable, false.B)
  io.alu_ctrl_out := RegNext(io.ctrl)
}
