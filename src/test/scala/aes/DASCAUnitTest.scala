package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class DASCAUnitTester(c: DASCA) extends PeekPokeTester(c) {

  private val dasca = c

  val plaintext = Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0)
  val roundKey = Array(0x53, 0x51, 0x12, 0x78, 0xa4, 0xb2, 0x3c, 0x96, 0x84, 0xd2, 0x81, 0xe4, 0xfa, 0x3c, 0x21, 0x79)
  val r = scala.util.Random

  // send the roundKey
  for (i <- 0 until Params.StateLength) {
    poke(dasca.io.roundKey(i), roundKey(i))
  }

  // start tests
  for (t <- 0 until 10000) {
    printf("test %d: \n", t)
    // generate plaintext
    for (i <- 0 until Params.StateLength) {
      plaintext(i) = r.nextInt(256)
    }
    // send the plaintext
    for (i <- 0 until Params.StateLength) {
      poke(dasca.io.plaintext(i), plaintext(i))
    }
    step(1)
  }

  step(4)
}

// Run test with:
// sbt 'testOnly aes.DASCATester'
// extend with the option '-- -z verbose' or '-- -z vcd' for specific test

class DASCATester extends ChiselFlatSpec {

  // private val Nk = 8 // 4, 6, 8 [32-bit words] columns in cipher key
  // private val SubBytes_SCD = true
  private val backendNames = Array("firrtl", "verilator")
  private val dir = "DASCA"

  for (backendName <- backendNames) {
    "DASCA" should s"execute DASCA (with $backendName)" in {
      Driver(() => new DASCA(), backendName) {
        c => new DASCAUnitTester(c)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir),
      () => new DASCA()) {
      c => new DASCAUnitTester(c)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
          "--backend-name", "verilator"), () => new DASCA()) {
        c => new DASCAUnitTester(c)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
          "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new DASCA()) {
        c => new DASCAUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
        "--is-verbose"), () => new DASCA()) {
      c => new DASCAUnitTester(c)
    } should be(true)
  }

}