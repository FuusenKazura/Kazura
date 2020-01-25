package kazura.stages

import chisel3._
import chisel3.util._
import kazura.models.{Ctrl, InstInfo}
import kazura.util.Params._
import kazura.modules._

class EXIO extends Bundle {
  // val source_dest: UInt = Input(UInt(1.W)) // 当面無視、マルチALU時に使用
  val inst_info: InstInfo = Input(new InstInfo)
  val source: Vec[UInt] = Input(Vec(RF.READ_PORT, UInt(LEN.W)))
  val branch_pc: UInt = Input(UInt(LEN.W))
  val next_pc: UInt = Input(UInt(LEN.W))
  val rd: UInt = Input(UInt(LEN.W))
  val predict: Bool = Input(Bool()) // trueなら分岐, falseなら分岐しない
  val pc: UInt = Input(UInt(LEN.W))

  val alu_out: UInt = Output(UInt(LEN.W))
  val inst_info_out: InstInfo = Output(new InstInfo)
  val restoration_pc_out: UInt = Output(UInt(LEN.W))
  val rd_out: UInt = Output(UInt(LEN.W))
  val pc_out: UInt = Output(UInt(LEN.W))
  val mispredicted: Bool = Output(Bool())
  // val alu_available: Vec[Bool] = Vec(1, Output(Bool())) // 当面無視、マルチALU時に使用
}

class EX extends Module {
  val io: EXIO = IO(new EXIO)
  val alu: ALU = Module(new ALU)
  alu.io.inst_info := io.inst_info
  alu.io.source := io.source
  alu.io.next_pc := io.next_pc
  alu.io.branch_pc := io.branch_pc
  alu.io.rd := io.rd
  alu.io.predict := io.predict
  alu.io.pc := io.pc
  // alu.io.source_enable := 1.U === io.source_dest
  alu.io.source_enable := true.B
  io.alu_out := alu.io.alu_out
  io.inst_info_out := alu.io.inst_info_out
  io.restoration_pc_out := alu.io.restoration_pc
  io.rd_out := alu.io.rd_out
  io.pc_out := alu.io.pc_out
  io.mispredicted := alu.io.mispredicted
  // io.alu_available := Wire(VecInit(true.B))
}
