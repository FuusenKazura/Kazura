package kazura.modules

import chisel3._
import chisel3.util._
import kazura.models.InstBits
import kazura.models.Inst._
import kazura.util.Params._

class IDIO extends Bundle {
  val inst_bits: InstBits = Input(new InstBits)
  val ctrl: Ctrl = Output(new Ctrl)
  val source_sel: Vec[UInt] = Vec(RF.READ_PORT, Output(UInt(LEN.W)))
}

class Ctrl extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val alu_op: UInt = UInt(ALUOP.NUM_W.W)
  val is_jump: Bool = Bool()
  val is_branch: Bool = Bool()
  val rf_w: Bool = Bool()
  val mem_r: Bool = Bool()
  val mem_w: Bool = Bool()
  val rs1_use: Bool = Bool()
  val rs2_use: Bool = Bool()
}

class Decoder extends Module {
  val io: IDIO = IO(new IDIO)
  val insts: Seq[Inst] = Seq(Add, Sub, And, Or, Addi, Subi, Incr, Incr, Decr, Ldi, Ld, St, Beq, Bgt, Jump, Nop)

  def conDecodeCell[A <: Inst](inst: A, instBits: InstBits): Unit = {
    io.ctrl.rd_addr   := instBits.rd
    io.ctrl.alu_op    := inst.alu_op
    io.ctrl.is_jump   := inst.is_jump
    io.ctrl.is_branch := inst.is_branch
    io.ctrl.rf_w      := inst.rf_w
    io.ctrl.mem_r     := inst.mem_r
    io.ctrl.mem_w     := inst.mem_w
    for (i <- 0 until RF.READ_PORT) {
      io.source_sel(i) := inst.source(i)
    }
    io.ctrl.rs1_use    := inst.rs1_use
    io.ctrl.rs2_use    := inst.rs2_use
  }

  conDecodeCell(Nop, io.inst_bits)
  insts.foldLeft(
    when(io.inst_bits.op === Nop.op) {
      conDecodeCell(Nop, io.inst_bits)
    })((d, inst) =>
    d.elsewhen(io.inst_bits.op === inst.op) {
      conDecodeCell(inst, io.inst_bits)
    })
}

