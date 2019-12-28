package kazura.stages

import chisel3._
import chisel3.util._
import kazura.util.Params._
import kazura.modules.{ALU, Ctrl}

class EXIO extends Bundle {
  val alu_op: UInt = Input(UInt(ALUOP.NUM_W.W))
  val source_dest: UInt = Input(UInt(1.W)) // 当面無視、マルチALU時に使用
  val ctrl: Ctrl = Input(new Ctrl)
  val source: Vec[UInt] = Input(Vec(RF.READ_PORT, UInt(LEN.W)))

  val alu_out: UInt = Output(UInt(LEN.W))
  val alu_ctrl_out: Ctrl = Output(new Ctrl)
  val alu_available: Vec[Bool] = Vec(1, Output(Bool))
}

class EX extends Module {
  val io: EXIO = IO(new EXIO)
  val alu: ALU = Module(new ALU)
  alu.io.ctrl := io.ctrl
  alu.io.source := io.source
  alu.io.source_enable := 1.U === io.source_dest
  io.alu_out := alu.io.alu_out
  io.alu_ctrl_out := alu.io.alu_ctrl_out
  io.alu_available := Wire(VecInit(true.B))
}
