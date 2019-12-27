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
  val cond_type: UInt = UInt(COND_TYPE.NUM_W.W)
  val rf_w: Bool = Bool()
  val mem_w: Bool = Bool()
  val pc_w: Bool = Bool()
}

class Decoder extends Module {
  val io: IDIO = IO(new IDIO)
  val insts: Seq[Inst] = Seq(Add, Sub, And, Or, Addi, Subi, Incr, Incr, Decr, Ldi, Ld, St, Beq, Bgt, Jump, Nop)

  def conDecodeCell[A <: Inst](inst: A, instBits: InstBits): Unit = {
    io.ctrl.rd_addr   := instBits.rd
    io.ctrl.alu_op    := inst.alu_op
    io.ctrl.cond_type := inst.cond_type
    io.ctrl.rf_w      := inst.rf_w
    io.ctrl.mem_w     := inst.mem_w
    io.ctrl.pc_w      := inst.pc_w
    io.source_sel     := inst.source
  }

  insts.foldLeft(
    when(io.inst_bits.op === Nop.op) {
      conDecodeCell(Nop, io.inst_bits)
    })((d, inst) =>
    d.elsewhen(io.inst_bits.op === inst.op) {
      conDecodeCell(inst, io.inst_bits)
    })
}

