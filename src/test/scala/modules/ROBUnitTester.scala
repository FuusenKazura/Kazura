package modules

import chisel3.iotesters._
import kazura.modules.ROB

class ROBUnitTester(m: ROB) extends PeekPokeTester(m) {
  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 0)
  poke(m.io.used_num, 4)
  for (i <- m.io.graduate.indices) { poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false) }
  step(1) // ------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 4)
  poke(m.io.used_num, 0)
  // for (i <- m.io.graduate.indices) poke(m.io.graduate(i).valid, false)
  poke(m.io.graduate(0).valid, false); poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(1).valid, false); poke(m.io.graduate(1).bits.mispredicted, false)
  poke(m.io.graduate(2).valid, true);  poke(m.io.graduate(2).bits.mispredicted, false)
  poke(m.io.graduate(2).bits.addr, 1)
  poke(m.io.graduate(2).bits.rd_addr, 1)
  poke(m.io.graduate(2).bits.alu_out, 10)
  poke(m.io.graduate(3).valid, false); poke(m.io.graduate(3).bits.mispredicted, false)
  step(1) // ------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 4)
  poke(m.io.used_num, 2)
  poke(m.io.graduate(0).valid, true);  poke(m.io.graduate(0).bits.mispredicted, false)
  poke(m.io.graduate(0).bits.addr, 0)
  poke(m.io.graduate(0).bits.rd_addr, 2)
  poke(m.io.graduate(0).bits.alu_out, 20)
  poke(m.io.graduate(1).valid, false); poke(m.io.graduate(1).bits.mispredicted, false)
  poke(m.io.graduate(2).valid, false); poke(m.io.graduate(2).bits.mispredicted, false)
  poke(m.io.graduate(3).valid, false); poke(m.io.graduate(3).bits.mispredicted, false)
  step(1) // ------------------------------

  expect(m.io.commit(0).rf_w, true)
  expect(m.io.commit(0).rd_addr, 2)
  expect(m.io.commit(0).data, 20)
  expect(m.io.commit(1).rf_w, true)
  expect(m.io.commit(1).data, 10)
  expect(m.io.commit(1).rd_addr, 1)
  expect(m.io.commit(2).rf_w, false)
  expect(m.io.commit(3).rf_w, false)

  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 6)
  poke(m.io.used_num, 0)
  for (i <- m.io.graduate.indices) { poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false) }
  step(1) // ------------------------------

  for (i <- m.io.commit.indices) expect(m.io.commit(i).rf_w, false)
  expect(m.io.unreserved_head.valid, true)
  expect(m.io.unreserved_head.bits, 6)
  poke(m.io.used_num, 4)
  for (i <- m.io.graduate.indices) { poke(m.io.graduate(i).valid, false); poke(m.io.graduate(i).bits.mispredicted, false) }
}
