package kazura.modules

import chisel3._
import chisel3.util._
import kazura.models.InstBits
import kazura.util.Params._

class IDIO extends Bundle {
  val inst: InstBits = Input(new InstBits)
  val ctrl: Ctrl = Output(new Ctrl)
}

class Ctrl extends Bundle {
  val rd_addr: UInt = UInt(RF.NUM_W.W)
  val alu_op: UInt = UInt(2.W)
  val cond_type: UInt = UInt(2.W)
  val rf_w: Bool = Bool()
  val mem_w: Bool = Bool()
  val pc_w: Bool = Bool()
  val source: Vec[UInt] = Vec(2, UInt(LEN.W))
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
    io.ctrl.source    := inst.source
  }

  insts.foldLeft(
    when(io.inst.op === Nop.op) {
      conDecodeCell(Nop, io.inst)
    })((d, inst) =>
    d.elsewhen(io.inst.op === inst.op) {
      conDecodeCell(inst, io.inst)
    })
}

sealed trait Inst        {
  val op: UInt;
  val alu_op: UInt;
  val cond_type: UInt;
  val rf_w:  Bool;
  val mem_w: Bool;
  val pc_w:  Bool;
  val source: Vec[UInt];
}
object Nop  extends Inst { val op="b0000".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.ZERO.U, Source2.ONE.U))}
object Add  extends Inst { val op="b0001".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.RS.U))}
object Sub  extends Inst { val op="b0010".U; val alu_op=ALUOP.SUB.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.RS.U))}
object And  extends Inst { val op="b0011".U; val alu_op=ALUOP.AND.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.RS.U))}
object Or   extends Inst { val op="b0100".U; val alu_op=ALUOP.OR.U;  val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.RS.U))}
object Addi extends Inst { val op="b0101".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.IMM9S.U))}
object Subi extends Inst { val op="b0110".U; val alu_op=ALUOP.SUB.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.IMM9S.U))}
object Incr extends Inst { val op="b0111".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.ONE.U))}
object Decr extends Inst { val op="b1000".U; val alu_op=ALUOP.SUB.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.RD.U, Source2.ONE.U))}
object Ldi  extends Inst { val op="b1001".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.DISP6U.U, Source2.RS.U))}
object Ld   extends Inst { val op="b1010".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=true.B;  val mem_w=false.B; val pc_w=false.B; val source=VecInit(Seq(Source1.DISP6U.U, Source2.RS.U))}
object St   extends Inst { val op="b1011".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NOP.U;            val rf_w=false.B; val mem_w=true.B;  val pc_w=false.B; val source=VecInit(Seq(Source1.DISP6U.U, Source2.RS.U))}
object Beq  extends Inst { val op="b1101".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.EQ.U;             val rf_w=false.B; val mem_w=false.B; val pc_w=true.B;  val source=VecInit(Seq(Source1.PC.U, Source2.DISP6S.U))}
object Bgt  extends Inst { val op="b1101".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.GT.U;             val rf_w=false.B; val mem_w=false.B; val pc_w=true.B;  val source=VecInit(Seq(Source1.PC.U, Source2.DISP6S.U))}
object Jump extends Inst { val op="b1110".U; val alu_op=ALUOP.ADD.U; val cond_type=COND_TYPE.NO_CONDITIONAL.U; val rf_w=false.B; val mem_w=false.B; val pc_w=true.B;  val source=VecInit(Seq(Source1.PC.U, Source2.IMM9S.U))}

