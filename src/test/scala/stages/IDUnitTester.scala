package stages

import kazura.stages.{ID, IFOut}
import kazura.util.Params._
import chisel3._
import chisel3.iotesters._
import chisel3.util.experimental.BoringUtils
import kazura.models.Inst
import kazura.models.InstBits

abstract class IDUnitTestHelper(m: ID) extends PeekPokeTester(m) {
  def setInst[I <: Inst.Inst](total_cnt: Int, pc: Int, inst: I, rd: Int, rs: Int, disp6u: Int): Unit = {
    poke(m.io.if_out.total_cnt, total_cnt)
    poke(m.io.if_out.pc, pc)
    poke(m.io.if_out.inst_bits.op, inst.op)
    poke(m.io.if_out.inst_bits.rd, rd)
    poke(m.io.if_out.inst_bits.rs, rs)
    poke(m.io.if_out.inst_bits.disp6u, disp6u)
  }
  case class WriteData(rd: Int, data: Int)
  def writeData(data: Seq[Option[WriteData]]): Unit = {
    for ((i, d) <- data.indices zip data) {
      poke(m.io.rf_write(i).rf_w, d.nonEmpty)
      poke(m.io.rf_write(i).rd_addr, d.map(_.rd).getOrElse(0))
      poke(m.io.rf_write(i).data, d.map(_.data).getOrElse(0))
    }
  }
}

class BusyBitUnitTest(m: ID) extends IDUnitTestHelper(m) {
  poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
  setInst(0, 0, Inst.Add, 0, 0, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0
  expect(m.io.stall, false, "依存が存在しないので、stallしない")
  poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
  setInst(1, 1, Inst.Add, 1, 0, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0, $1
  expect(m.io.stall, false, "$0は特例でstallしない(常に0な為)")
  poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
  setInst(2, 2, Inst.Add, 2, 1, 0)
  writeData(Seq.fill(RF.WRITE_PORT)(None))
  step(1)
  // queue: $0, $1
  expect(m.io.stall,true, "$1に依存しているのでstall")
  // この命令は無視される
  poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
  setInst(3, 3, Inst.Add, 5, 5, 0)
  writeData(Seq(Some(WriteData(1, 1))) ++ Seq.fill(RF.WRITE_PORT - 1)(None))
  step(1)
  // queue: $0
  expect(m.io.stall,false, "$1に依存が解消されたのでstallしない")
}

class RFUnitTester(m: ID) extends IDUnitTestHelper(m) {
  for (i <- 0 until RF.NUM by RF.WRITE_PORT) {
    poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
    setInst(i, i, Inst.Nop, 0, 0, 0)
    (0 until RF.WRITE_PORT).foreach(x => println(s"${i+x} <- ${i+x}"))
    writeData((0 until RF.WRITE_PORT).map(x => Some(WriteData(i+x, i+x))))
    step(1)
  }
  for (i <- 0 until RF.NUM) {
    poke(m.io.predict, false); poke(m.io.branch_mispredicted_enable, false); poke(m.io.branch_mispredicted, false)
    setInst(i, i, Inst.Add, i, i, 0)
    writeData(Seq.fill(RF.WRITE_PORT)(None))
    step(1)
    expect(m.io.source(0), i, s"rf(${i}) != i (書き込んだ値と読み出された値が違う)")
  }
}
