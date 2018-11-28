package lfsr

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class LFSRUnitTester(c: LFSR) extends PeekPokeTester(c) {
  //init D0123456 to 1
  var D0123456 = 1
  for (t <- 0 until 63) {
    step(1)
    val bit = ((D0123456 >> 5) ^ (D0123456 >> 4)) & 1;
    D0123456 = (D0123456 << 1) | bit;
    if (D0123456 > 63) {
      D0123456 = D0123456 - 64
    }
    expect(c.io.lfsr_6, D0123456)
  }
}

// Run with:
//    sbt 'testOnly lfsr.LFSRTester'
// or sbt 'testOnly lfsr.LFSRTester -- -z verbose'
// or sbt 'testOnly lfsr.LFSRTester -- -z vcd'

class LFSRTester extends ChiselFlatSpec {

  private val backendNames = Array[String]("firrtl", "verilator")
  private val dir = "LFSR"

  for (backendName <- backendNames) {
    "LFSR" should s"calculate proper greatest common denominator (with $backendName)" in {
      Driver(() => new LFSR, backendName) {
        c => new LFSRUnitTester(c)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir), () => new LFSR) {
      c => new LFSRUnitTester(c)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
        "--backend-name", "verilator"), () => new LFSR) {
        c => new LFSRUnitTester(c)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
        "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new LFSR) {
        c => new LFSRUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
      "--is-verbose"), () => new LFSR) {
      c => new LFSRUnitTester(c)
    } should be(true)
  }

}
