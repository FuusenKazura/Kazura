package kazura.modules

import chisel3._
import chisel3.util.MuxLookup
import kazura.util.Params._
import kazura.models._

class ALUIO extends Bundle {
  val inst_info: InstInfo = Input(new InstInfo)
  val source: Vec[UInt] = Vec(2, Input(UInt(LEN.W)))
  val source_enable: Bool = Input(Bool())
  val next_pc: UInt = Input(UInt(LEN.W))
  val branch_pc: UInt = Input(UInt(LEN.W))
  val rd: UInt = Input(UInt(LEN.W))
  val predict: Bool = Input(Bool()) // trueなら分岐, falseなら分岐しない
  val pc: UInt = Input(UInt(LEN.W))

  val alu_out: UInt = Output(UInt(LEN.W))
  val inst_info_out: InstInfo = Output(new InstInfo)
  val restoration_pc: UInt = Output(UInt(LEN.W))
  val rd_out: UInt = Output(UInt(LEN.W))
  val pc_out: UInt = Output(UInt(LEN.W))
  val mispredicted: Bool = Output(Bool())
}

class ALU extends Module {
  val io: ALUIO = IO(new ALUIO)
  val alu_out: UInt = MuxLookup(io.inst_info.ctrl.alu_op, 0.U, Seq(
    ALUOP.ADD.U -> io.source(0).+(io.source(1)),
    ALUOP.SUB.U -> io.source(0).-(io.source(1)),
    ALUOP.AND.U -> io.source(0).&(io.source(1)),
    ALUOP.OR.U  -> io.source(0).|(io.source(1)),
    ALUOP.BEQ.U  -> io.source(0).===(io.source(1)),
    ALUOP.BGT.U  -> io.source(0).>(io.source(1))
  ))
  io.alu_out := RegNext(alu_out)
  io.inst_info_out.rd_addr := RegNext(io.inst_info.rd_addr)
  io.inst_info_out.ctrl := RegNext(io.inst_info.ctrl, Ctrl.nop)
  io.restoration_pc := RegNext(Mux(io.predict,
    io.next_pc, // TODO: 整理
    io.branch_pc
  ))
  io.rd_out := RegNext(io.rd)
  io.pc_out := RegNext(io.pc)
  io.mispredicted := RegNext(io.predict =/= alu_out)
}
