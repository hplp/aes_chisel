package aes

import chisel3._
import chisel3.util._

// implements wrapper for AES cipher and inverse cipher
// change Nk=4 for AES128, NK=6 for AES192, Nk=8 for AES256
// change expandedKeyMemType= ROM, Mem, SyncReadMem
class AES(Nk: Int, unrolled: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String) extends Module {
  require(Nk == 4 || Nk == 6 || Nk == 8)
  require(expandedKeyMemType == "ROM" || expandedKeyMemType == "Mem" || expandedKeyMemType == "SyncReadMem")
  val KeyLength: Int = Nk * Params.rows
  val Nr: Int = Nk + 6 // 10, 12, 14 rounds
  val Nrplus1: Int = Nr + 1 // 10+1, 12+1, 14+1
  val EKDepth: Int = 16 // enough memory for any expanded key

  val io = IO(new Bundle {
    val AES_mode = Input(UInt(2.W)) //  0=00=off, 1=01=expanded key update, 2=10=cipher, 3=11=inverse cipher
    //
    val input_text = Input(Vec(Params.StateLength, UInt(8.W))) // plaintext, ciphertext, roundKey
    //
    val output_text = Output(Vec(Params.StateLength, UInt(8.W))) // ciphertext or plaintext
    val output_valid = Output(Bool())
  })

  // Instantiate module objects
  val CipherModule = Cipher(Nk, SubBytes_SCD)
  val InvCipherModule = InvCipher(Nk, InvSubBytes_SCD)

  // A roundKey is Params.StateLength bytes, and 1+(10/12/14) (< EKDepth) of them are needed
  // Mem = combinational/asynchronous-read, sequential/synchronous-write = register banks
  // Create a asynchronous-read, synchronous-write memory block big enough for any key length
  val expandedKeyARMem = Mem(EKDepth, Vec(Params.StateLength, UInt(8.W)))

  // SyncReadMem = sequential/synchronous-read, sequential/synchronous-write = SRAMs
  // Create a synchronous-read, synchronous-write memory block big enough for any key length
  val expandedKeySRMem = SyncReadMem(EKDepth, Vec(Params.StateLength, UInt(8.W)))

  // use the same address and dataOut val elements to interface with the parameterized memory
  val address = RegInit(0.U(log2Ceil(EKDepth).W))
  val dataOut = Wire(Vec(Params.StateLength, UInt(8.W)))

  when(io.AES_mode === 1.U) { // write to memory
    if (expandedKeyMemType == "Mem") {
      expandedKeyARMem.write(address, io.input_text)
    }
    else if (expandedKeyMemType == "SyncReadMem") {
      expandedKeySRMem.write(address, io.input_text)
    }
    dataOut := DontCare
    address := address + 1.U
  }
    .otherwise { // read from memory
      if (expandedKeyMemType == "Mem") {
        dataOut := expandedKeyARMem.read(address)
      }
      else if (expandedKeyMemType == "SyncReadMem") {
        dataOut := expandedKeySRMem.read(address)
      }
      else if (expandedKeyMemType == "ROM") {
        Nk match {
          case 4 => dataOut := ROMeKeys.expandedKey128(address)
          case 6 => dataOut := ROMeKeys.expandedKey192(address)
          case 8 => dataOut := ROMeKeys.expandedKey256(address)
        }
      }

      // address logistics
      when(
        if ((expandedKeyMemType == "Mem") || (expandedKeyMemType == "ROM")) {
          (ShiftRegister(io.AES_mode, 1) === 2.U) // delay by 1 for Mem and ROM
        }
        else {
          (io.AES_mode === 2.U) // no delay for SyncReadMem
        }
      ) {
        address := address + 1.U
      }
        .elsewhen(io.AES_mode === 3.U) {
          when(address === 0.U) {
            address := Nr.U
          }
            .otherwise {
              address := address - 1.U
            }
        }
        .otherwise {
          address := 0.U
        }
    }

  // The roundKey for each round can go to both the cipher and inverse cipher (for now TODO)
  CipherModule.io.roundKey := dataOut
  InvCipherModule.io.roundKey := dataOut

  // The input text can go to both the cipher and the inverse cipher (for now TODO)
  CipherModule.io.plaintext <> io.input_text
  InvCipherModule.io.ciphertext <> io.input_text

  // Cipher starts at AES_Mode=2
  CipherModule.io.start := (io.AES_mode === 2.U)
  // Inverse Cipher starts at AES_Mode=3 and address=Nr
  if (expandedKeyMemType == "SyncReadMem") {
    InvCipherModule.io.start := (ShiftRegister(io.AES_mode, 1) === 3.U) // delay by 1 for SyncReadMem
  }
  else {
    InvCipherModule.io.start := (io.AES_mode === 3.U) // no delay for Mem and ROM
  }

  // AES output_valid can be the Cipher.output_valid OR InvCipher.output_valid
  io.output_valid := CipherModule.io.state_out_valid || InvCipherModule.io.state_out_valid
  // AES output can be managed using a Mux on the Cipher output and the InvCipher output
  io.output_text <> Mux(CipherModule.io.state_out_valid, CipherModule.io.state_out, InvCipherModule.io.state_out)

  // Debug statements
  //  printf("AES mode=%b, mem_address=%d, mem_dataOut=%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x%x \n", io.AES_mode, address, dataOut(0), dataOut(1), dataOut(2), dataOut(3), dataOut(4), dataOut(5), dataOut(6), dataOut(7), dataOut(8), dataOut(9), dataOut(10), dataOut(11), dataOut(12), dataOut(13), dataOut(14), dataOut(15))
}

object AES {
  def apply(Nk: Int, unrolled: Int, SubBytes_SCD: Boolean, InvSubBytes_SCD: Boolean, expandedKeyMemType: String): AES = Module(new AES(Nk, unrolled, SubBytes_SCD, InvSubBytes_SCD, expandedKeyMemType))
}