package aes

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class AESUnitTester(c: AES, Nk: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean) extends PeekPokeTester(c) {
  require(Nk == 4 || Nk == 6 || Nk == 8)

  private val aes_i = c

  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1

  val input_text = Array(0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d, 0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34)

  val roundKey128 = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f)
  val expandedKey128 = Array(
    Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f),
    Array(0xd6, 0xaa, 0x74, 0xfd, 0xd2, 0xaf, 0x72, 0xfa, 0xda, 0xa6, 0x78, 0xf1, 0xd6, 0xab, 0x76, 0xfe),
    Array(0xb6, 0x92, 0xcf, 0x0b, 0x64, 0x3d, 0xbd, 0xf1, 0xbe, 0x9b, 0xc5, 0x00, 0x68, 0x30, 0xb3, 0xfe),
    Array(0xb6, 0xff, 0x74, 0x4e, 0xd2, 0xc2, 0xc9, 0xbf, 0x6c, 0x59, 0x0c, 0xbf, 0x04, 0x69, 0xbf, 0x41),
    Array(0x47, 0xf7, 0xf7, 0xbc, 0x95, 0x35, 0x3e, 0x03, 0xf9, 0x6c, 0x32, 0xbc, 0xfd, 0x05, 0x8d, 0xfd),
    Array(0x3c, 0xaa, 0xa3, 0xe8, 0xa9, 0x9f, 0x9d, 0xeb, 0x50, 0xf3, 0xaf, 0x57, 0xad, 0xf6, 0x22, 0xaa),
    Array(0x5e, 0x39, 0x0f, 0x7d, 0xf7, 0xa6, 0x92, 0x96, 0xa7, 0x55, 0x3d, 0xc1, 0x0a, 0xa3, 0x1f, 0x6b),
    Array(0x14, 0xf9, 0x70, 0x1a, 0xe3, 0x5f, 0xe2, 0x8c, 0x44, 0x0a, 0xdf, 0x4d, 0x4e, 0xa9, 0xc0, 0x26),
    Array(0x47, 0x43, 0x87, 0x35, 0xa4, 0x1c, 0x65, 0xb9, 0xe0, 0x16, 0xba, 0xf4, 0xae, 0xbf, 0x7a, 0xd2),
    Array(0x54, 0x99, 0x32, 0xd1, 0xf0, 0x85, 0x57, 0x68, 0x10, 0x93, 0xed, 0x9c, 0xbe, 0x2c, 0x97, 0x4e),
    Array(0x13, 0x11, 0x1d, 0x7f, 0xe3, 0x94, 0x4a, 0x17, 0xf3, 0x07, 0xa7, 0x8b, 0x4d, 0x2b, 0x30, 0xc5))

  val roundKey192 = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17)
  val expandedKey192 = Array(
    Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f),
    Array(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x58, 0x46, 0xf2, 0xf9, 0x5c, 0x43, 0xf4, 0xfe),
    Array(0x54, 0x4a, 0xfe, 0xf5, 0x58, 0x47, 0xf0, 0xfa, 0x48, 0x56, 0xe2, 0xe9, 0x5c, 0x43, 0xf4, 0xfe),
    Array(0x40, 0xf9, 0x49, 0xb3, 0x1c, 0xba, 0xbd, 0x4d, 0x48, 0xf0, 0x43, 0xb8, 0x10, 0xb7, 0xb3, 0x42),
    Array(0x58, 0xe1, 0x51, 0xab, 0x04, 0xa2, 0xa5, 0x55, 0x7e, 0xff, 0xb5, 0x41, 0x62, 0x45, 0x08, 0x0c),
    Array(0x2a, 0xb5, 0x4b, 0xb4, 0x3a, 0x02, 0xf8, 0xf6, 0x62, 0xe3, 0xa9, 0x5d, 0x66, 0x41, 0x0c, 0x08),
    Array(0xf5, 0x01, 0x85, 0x72, 0x97, 0x44, 0x8d, 0x7e, 0xbd, 0xf1, 0xc6, 0xca, 0x87, 0xf3, 0x3e, 0x3c),
    Array(0xe5, 0x10, 0x97, 0x61, 0x83, 0x51, 0x9b, 0x69, 0x34, 0x15, 0x7c, 0x9e, 0xa3, 0x51, 0xf1, 0xe0),
    Array(0x1e, 0xa0, 0x37, 0x2a, 0x99, 0x53, 0x09, 0x16, 0x7c, 0x43, 0x9e, 0x77, 0xff, 0x12, 0x05, 0x1e),
    Array(0xdd, 0x7e, 0x0e, 0x88, 0x7e, 0x2f, 0xff, 0x68, 0x60, 0x8f, 0xc8, 0x42, 0xf9, 0xdc, 0xc1, 0x54),
    Array(0x85, 0x9f, 0x5f, 0x23, 0x7a, 0x8d, 0x5a, 0x3d, 0xc0, 0xc0, 0x29, 0x52, 0xbe, 0xef, 0xd6, 0x3a),
    Array(0xde, 0x60, 0x1e, 0x78, 0x27, 0xbc, 0xdf, 0x2c, 0xa2, 0x23, 0x80, 0x0f, 0xd8, 0xae, 0xda, 0x32),
    Array(0xa4, 0x97, 0x0a, 0x33, 0x1a, 0x78, 0xdc, 0x09, 0xc4, 0x18, 0xc2, 0x71, 0xe3, 0xa4, 0x1d, 0x5d))

  val roundKey256 = Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f)
  val expandedKey256 = Array(
    Array(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f),
    Array(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f),
    Array(0xa5, 0x73, 0xc2, 0x9f, 0xa1, 0x76, 0xc4, 0x98, 0xa9, 0x7f, 0xce, 0x93, 0xa5, 0x72, 0xc0, 0x9c),
    Array(0x16, 0x51, 0xa8, 0xcd, 0x02, 0x44, 0xbe, 0xda, 0x1a, 0x5d, 0xa4, 0xc1, 0x06, 0x40, 0xba, 0xde),
    Array(0xae, 0x87, 0xdf, 0xf0, 0x0f, 0xf1, 0x1b, 0x68, 0xa6, 0x8e, 0xd5, 0xfb, 0x03, 0xfc, 0x15, 0x67),
    Array(0x6d, 0xe1, 0xf1, 0x48, 0x6f, 0xa5, 0x4f, 0x92, 0x75, 0xf8, 0xeb, 0x53, 0x73, 0xb8, 0x51, 0x8d),
    Array(0xc6, 0x56, 0x82, 0x7f, 0xc9, 0xa7, 0x99, 0x17, 0x6f, 0x29, 0x4c, 0xec, 0x6c, 0xd5, 0x59, 0x8b),
    Array(0x3d, 0xe2, 0x3a, 0x75, 0x52, 0x47, 0x75, 0xe7, 0x27, 0xbf, 0x9e, 0xb4, 0x54, 0x07, 0xcf, 0x39),
    Array(0x0b, 0xdc, 0x90, 0x5f, 0xc2, 0x7b, 0x09, 0x48, 0xad, 0x52, 0x45, 0xa4, 0xc1, 0x87, 0x1c, 0x2f),
    Array(0x45, 0xf5, 0xa6, 0x60, 0x17, 0xb2, 0xd3, 0x87, 0x30, 0x0d, 0x4d, 0x33, 0x64, 0x0a, 0x82, 0x0a),
    Array(0x7c, 0xcf, 0xf7, 0x1c, 0xbe, 0xb4, 0xfe, 0x54, 0x13, 0xe6, 0xbb, 0xf0, 0xd2, 0x61, 0xa7, 0xdf),
    Array(0xf0, 0x1a, 0xfa, 0xfe, 0xe7, 0xa8, 0x29, 0x79, 0xd7, 0xa5, 0x64, 0x4a, 0xb3, 0xaf, 0xe6, 0x40),
    Array(0x25, 0x41, 0xfe, 0x71, 0x9b, 0xf5, 0x00, 0x25, 0x88, 0x13, 0xbb, 0xd5, 0x5a, 0x72, 0x1c, 0x0a),
    Array(0x4e, 0x5a, 0x66, 0x99, 0xa9, 0xf2, 0x4f, 0xe0, 0x7e, 0x57, 0x2b, 0xaa, 0xcd, 0xf8, 0xcd, 0xea),
    Array(0x24, 0xfc, 0x79, 0xcc, 0xbf, 0x09, 0x79, 0xe9, 0x37, 0x1a, 0xc2, 0x3c, 0x6d, 0x68, 0xde, 0x36))

  val roundKey = Nk match {
    case 4 => roundKey128
    case 6 => roundKey192
    case 8 => roundKey256
  }

  val expandedKey = Nk match {
    case 4 => expandedKey128
    case 6 => expandedKey192
    case 8 => expandedKey256
  }

  printf("\nStarting the tests with 4 idle cycles\n")
  poke(aes_i.io.AES_mode, 0) // off
  poke(aes_i.io.start, 0) // off
  step(4) // test that things are fine in Idle state

  printf("\nSending expanded AES key\n")
  // send expanded key to AES memory block
  poke(aes_i.io.AES_mode, 1) // configure key
  for (i <- 0 until Nrplus1) {
    for (j <- 0 until Params.StateLength) {
      poke(aes_i.io.input_text(j), expandedKey(i)(j))
    }
    step(1)
  }

  printf("\nStaying idle for 4 cycles\n")
  poke(aes_i.io.AES_mode, 0) // must stop when all roundKeys were sent
  step(4)

  printf("\nStarting AES cipher mode, sending plaintext\n")
  poke(aes_i.io.AES_mode, 2) // cipher
  step(1)
  poke(aes_i.io.start, 1) // send start
  step(1)

  // send the plaintext
  for (i <- 0 until Params.StateLength) {
    poke(aes_i.io.input_text(i), input_text(i))
  }
  poke(aes_i.io.start, 0) // reset start
  step(1)

  // remaining rounds
  for (i <- 1 until Nrplus1) {
    step(1)
  }

  val state_e128 = Array(0x89, 0xed, 0x5e, 0x6a, 0x05, 0xca, 0x76, 0x33, 0x81, 0x35, 0x08, 0x5f, 0xe2, 0x1c, 0x40, 0xbd)
  val state_e192 = Array(0xbc, 0x3a, 0xaa, 0xb5, 0xd9, 0x7b, 0xaa, 0x7b, 0x32, 0x5d, 0x7b, 0x8f, 0x69, 0xcd, 0x7c, 0xa8)
  val state_e256 = Array(0x9a, 0x19, 0x88, 0x30, 0xff, 0x9a, 0x4e, 0x39, 0xec, 0x15, 0x01, 0x54, 0x7d, 0x4a, 0x6b, 0x1b)

  val state_e = Nk match {
    case 4 => state_e128
    case 6 => state_e192
    case 8 => state_e256
  }

  printf("\nInspecting cipher output\n")
  // verify aes cipher output
  for (i <- 0 until Params.StateLength)
    expect(aes_i.io.output_text(i), state_e(i))
  expect(aes_i.io.output_valid, 1)

  // store cipher output
  val cipher_output = peek(aes_i.io.output_text)

  printf("\nStaying idle for 4 cycles\n")
  poke(aes_i.io.AES_mode, 0) // off
  step(4)

  printf("\nStarting AES inverse cipher mode, sending ciphertext\n")
  poke(aes_i.io.AES_mode, 3) // inverse cipher
  step(3)
  poke(aes_i.io.start, 1) // send start
  step(1)

  // send the ciphertext
  for (i <- 0 until Params.StateLength) {
    poke(aes_i.io.input_text(i), cipher_output(i)) // same as state_e(i)
  }
  // reset start
  poke(aes_i.io.start, 0)
  step(1)

  // remaining rounds
  for (i <- 1 until Nrplus1) {
    step(1)
  }

  printf("\nInspecting inverse cipher output\n")
  // verify aes cipher output
  for (i <- 0 until Params.StateLength)
    expect(aes_i.io.output_text(i), input_text(i))
  expect(aes_i.io.output_valid, 1)

  printf("\nStaying idle for 4 cycles\n")
  step(4)
}

// Run test with:
// sbt 'testOnly aes.AESTester'
// sbt 'testOnly aes.AESTester -- -z "using verilator"'
// sbt 'testOnly aes.AESTester -- -z "using firrtl"'
// sbt 'testOnly aes.AESTester -- -z verbose'
// sbt 'testOnly aes.AESTester -- -z vcd'

class AESTester extends ChiselFlatSpec {

  private val SubBytes_SCD = false
  private val InvSubBytes_SCD = false
  val Nk = 8 // 4, 6, 8 [32-bit words] columns in cipher key
  private val backendNames = Array("firrtl", "verilator")
  private val dir = "AES"

  for (backendName <- backendNames) {
    "AES" should s"execute cipher and inverse cipher (with $backendName)" in {
      Driver(() => new AES(Nk, SubBytes_SCD, InvSubBytes_SCD), backendName) {
        c => new AESUnitTester(c, Nk, SubBytes_SCD, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_basic_test", "--top-name", dir),
      () => new AES(Nk, SubBytes_SCD, InvSubBytes_SCD)) {
      c => new AESUnitTester(c, Nk, SubBytes_SCD, InvSubBytes_SCD)
    } should be(true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if (backendNames.contains("verilator")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_verilator_test", "--top-name", dir,
          "--backend-name", "verilator"), () => new AES(Nk, SubBytes_SCD, InvSubBytes_SCD)) {
        c => new AESUnitTester(c, Nk, SubBytes_SCD, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "using --backend-name firrtl" should "be an alternative way to run using firrtl" in {
    if (backendNames.contains("firrtl")) {
      iotesters.Driver.execute(
        Array("--target-dir", "test_run_dir/" + dir + "_firrtl_test", "--top-name", dir,
          "--backend-name", "firrtl", "--generate-vcd-output", "on"), () => new AES(Nk, SubBytes_SCD, InvSubBytes_SCD)) {
        c => new AESUnitTester(c, Nk, SubBytes_SCD, InvSubBytes_SCD)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(
      Array("--target-dir", "test_run_dir/" + dir + "_verbose_test", "--top-name", dir,
        "--is-verbose"), () => new AES(Nk, SubBytes_SCD, InvSubBytes_SCD)) {
      c => new AESUnitTester(c, Nk, SubBytes_SCD, InvSubBytes_SCD)
    } should be(true)
  }

}
