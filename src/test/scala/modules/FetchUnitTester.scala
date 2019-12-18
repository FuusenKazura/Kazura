package modules

import chisel3.iotesters.PeekPokeTester
import kazura.modules.Fetch

class FetchUnitTester(m: Fetch) extends PeekPokeTester(m) {
  poke(m.io.in.alu_out, 127)
  poke(m.io.in.is_branch_or_jump_detected, false)
  poke(m.io.in.stole, false)

  expect(m.io.out.pc, 0)
  expect(m.io.out.total_cnt, 0)
  expect(m.io.out.inst.op, 0);
  expect(m.io.out.inst.rd, 0);
  expect(m.io.out.inst.rs, 0);
  expect(m.io.out.inst.disp6u, 10);

  step(1) // ---------------------------------------------

  poke(m.io.in.alu_out, 63)
  poke(m.io.in.is_branch_or_jump_detected, false)
  poke(m.io.in.stole, true)

  expect(m.io.out.pc, 1)
  expect(m.io.out.total_cnt, 1)
  expect(m.io.out.inst.op, 0);
  expect(m.io.out.inst.rd, 0);
  expect(m.io.out.inst.rs, 0);
  expect(m.io.out.inst.disp6u, 11);

  step(1) // ---------------------------------------------

  poke(m.io.in.alu_out, 31)
  poke(m.io.in.is_branch_or_jump_detected, false)
  poke(m.io.in.stole, false)

  expect(m.io.out.pc, 1)
  expect(m.io.out.total_cnt, 2)
  expect(m.io.out.inst.op, 0);
  expect(m.io.out.inst.rd, 0);
  expect(m.io.out.inst.rs, 0);
  expect(m.io.out.inst.disp6u, 11);

  step(1) // ---------------------------------------------

  poke(m.io.in.alu_out, 15)
  poke(m.io.in.is_branch_or_jump_detected, true)
  poke(m.io.in.stole, false)

  expect(m.io.out.pc, 2)
  expect(m.io.out.total_cnt, 3)
  expect(m.io.out.inst.op, 0);
  expect(m.io.out.inst.rd, 0);
  expect(m.io.out.inst.rs, 0);
  expect(m.io.out.inst.disp6u, 12);

  step(1) // ---------------------------------------------

  poke(m.io.in.alu_out, 25)
  poke(m.io.in.is_branch_or_jump_detected, true)
  poke(m.io.in.stole, false)

  expect(m.io.out.pc, 17)
  expect(m.io.out.total_cnt, 4)
  expect(m.io.out.inst.op, 0);
  expect(m.io.out.inst.rd, 0);
  expect(m.io.out.inst.rs, 0);
  expect(m.io.out.inst.disp6u, 27);
}
