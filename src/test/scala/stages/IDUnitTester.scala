package stages

import kazura.stages.{ID, IFOut}
import kazura.util.Params._
import chisel3.iotesters._
import kazura.models.Inst
import kazura.models.InstBits

class BusyBitUnitTest(m: ID) extends PeekPokeTester(m) {
  def setInst[I <: Inst.Inst](total_cnt: Int, pc: Int, inst: I, rd: Int, rs: Int, disp6u: Int): Unit = {
    poke(m.io.if_out.total_cnt, total_cnt)
    poke(m.io.if_out.pc, pc)
    poke(m.io.if_out.inst_bits.op, inst.op)
    poke(m.io.if_out.inst_bits.rd, rd)
    poke(m.io.if_out.inst_bits.rs, rs)
    poke(m.io.if_out.inst_bits.disp6u, disp6u)
  }
  def writeData(data: Seq[Option[(Int, Int)]]): Unit = {
    for ((i, d) <- data.indices zip data) {
      poke(m.io.rf_write(i).rf_w, d.nonEmpty)
      poke(m.io.rf_write(i).rd_addr, d.map(_._1).getOrElse(0))
      poke(m.io.rf_write(i).data, d.map(_._2).getOrElse(0))
    }
  }

  poke(m.io.predict, false); poke(m.io.branch_end, false); poke(m.io.branch_mispredicted, false)
  setInst(0, 0, Inst.Add, 0, 0, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0
  expect(m.io.stall, false, "依存が存在しないので、stallしない")
  poke(m.io.predict, false); poke(m.io.branch_end, false); poke(m.io.branch_mispredicted, false)
  setInst(1, 1, Inst.Add, 1, 0, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0, $1
  expect(m.io.stall, false, "$0は特例でstallしない(常に0な為)")
  poke(m.io.predict, false); poke(m.io.branch_end, false); poke(m.io.branch_mispredicted, false)
  setInst(2, 2, Inst.Add, 2, 1, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0, $1
  expect(m.io.stall,true, "$1に依存しているのでstall")
  // poke(m.io.predict, false); poke(m.io.branch_end, false); poke(m.io.branch_mispredicted, false)
  // setInst(pc, total_cnt, Inst.Add, 1, 0, 0)
  // writeData(Seq.fill(RF.WRITE_PORT)(None))
  // step(0)
}
