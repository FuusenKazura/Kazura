package kazura.models

import chisel3._
import kazura.util.Params._

object Inst {
  sealed trait Inst {
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
}
