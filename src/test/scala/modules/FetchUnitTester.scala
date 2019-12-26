package modules

import chisel3.iotesters.PeekPokeTester
import kazura.modules.Fetch

class FetchUnitTester(m: Fetch) extends PeekPokeTester(m) {
  poke(m.io.in.prev_pc, 0)
  poke(m.io.in.prev_total_cnt, 0)
  poke(m.io.in.alu_out, 127)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 1)
  expect(m.io.out.total_cnt, 1)

  step(1) // ---------------------------------------------
  // stall test

  poke(m.io.in.prev_pc, 1)
  poke(m.io.in.prev_total_cnt, 1)
  poke(m.io.in.alu_out, 63)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, true)

  expect(m.io.out.pc, 1)
  expect(m.io.out.total_cnt, 2)

  step(1) // ---------------------------------------------
  // branch test

  poke(m.io.in.prev_pc, 1)
  poke(m.io.in.prev_total_cnt, 2)
  poke(m.io.in.alu_out, 15)
  poke(m.io.in.is_branch, true)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 16)
  expect(m.io.out.total_cnt, 3)

  step(1) // ---------------------------------------------

  poke(m.io.in.prev_pc, 16)
  poke(m.io.in.prev_total_cnt, 3)
  poke(m.io.in.alu_out, 31)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, false)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 17)
  expect(m.io.out.total_cnt, 4)

  step(1) // ---------------------------------------------
  // jump test

  poke(m.io.in.prev_pc, 17)
  poke(m.io.in.prev_total_cnt, 4)
  poke(m.io.in.alu_out, 10)
  poke(m.io.in.is_branch, false)
  poke(m.io.in.is_jump, true)
  poke(m.io.in.stall, false)

  expect(m.io.out.pc, 27)
  expect(m.io.out.total_cnt, 5)
}
