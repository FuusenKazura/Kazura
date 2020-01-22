package modules

import chisel3.iotesters._
import kazura.modules.ROB

class ROBUnitTester(m: ROB) extends PeekPokeTester(m) {
  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 0)
  poke(m.io.used_num, 4) // ４つreserve
  for (i <- m.io.graduate.indices) {
    poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false)
  }
  step(1) //------------------------------------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 4)
  poke(m.io.used_num, 0)
  // rob(1)の命令をgraduate, rob(0)がまだなのでcommitは不可能
  for (i <- Seq(0, 1, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  // rob(0)の命令をgraduate, rob(0)がまだなのでcommitは不可能
  poke(m.io.graduate(2).valid, true)
  poke(m.io.graduate(2).bits.mispredicted, false)
  poke(m.io.graduate(2).bits.ctrl.rf_w, true)
  poke(m.io.graduate(2).bits.addr, 1)
  poke(m.io.graduate(2).bits.ctrl.rd_addr, 1)
  poke(m.io.graduate(2).bits.data, 10)
  step(1) // ------------------------------------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 4)
  poke(m.io.used_num, 3)
  for (i <- Seq(1, 2, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  // rob(1)の命令をgraduate, これでrob(1)以下の命令をcommit可能に
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(0).bits.ctrl.rf_w, true)
  poke(m.io.graduate(0).bits.addr, 0)
  poke(m.io.graduate(0).bits.ctrl.rd_addr, 2)
  poke(m.io.graduate(0).bits.data, 20)
  step(1) // ------------------------------------------------------------

  expect(m.io.commit(0).rf_w, true)
  expect(m.io.commit(0).rd_addr, 2)
  expect(m.io.commit(0).data, 20)
  expect(m.io.commit(1).rf_w, true)
  expect(m.io.commit(1).data, 10)
  expect(m.io.commit(1).rd_addr, 1)
  expect(m.io.commit(2).rf_w, false)
  expect(m.io.commit(3).rf_w, false)

  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 7)

  poke(m.io.used_num, 0)
  poke(m.io.graduate(0).valid, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.addr, 6)
  poke(m.io.graduate(0).bits.mispredicted, true)
  poke(m.io.graduate(0).bits.mispredicted, true)
  for (i <- Seq(1, 2, 3)) {
    poke(m.io.graduate(i).valid, false)
    poke(m.io.graduate(i).bits.mispredicted, false)
  }
  step(1) // ------------------------------------------------------------

  // TODO: mispredicted時のテストを書く

  for (i <- 0 until 4)
    expect(m.io.commit(i).rf_w, false)

  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 6)
}
