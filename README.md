# Implementation of the Advanced Encryption Standard (AES) in [Chisel](https://chisel.eecs.berkeley.edu/)


## Motivation
* Implement an open-source, transparent, secure encryption module in Chisel
* Equip developers with ready-to-use, efficient, parameterizable, encryption macros for RISC-V systems
* Compare performance and resource utilization of generated HDL with pure Verilog and Vivado HLS
* Compare code size with Verilog, Python, C++ and Vivado HLS as a index of development productivity

## Features
* _(more to come)_
* Compact AES Cipher and Inverse Cipher Chisel implementation
* Basic side-channel attack defense based on LFSR noise
* IDE (IntelliJ) compatible

## Instructions
* Run `./aes_tests.sh > logfile.txt` to run all tests and store console output into logfile
* Generated Verilog will be in directory _/test_run_dir_
* Either [gtkwave](http://gtkwave.sourceforge.net/) or [dinotrace](https://www.veripool.org/wiki/dinotrace) (or other wave viewer) can be used to display _.vcd_ files

## References
* Our implementations of AES in Python, C/C++, Vivado HLS: [github.com/hplp/AES_implementations](https://github.com/hplp/AES_implementations)
* This project was created using the [chisel-template](https://github.com/freechipsproject/chisel-template) from [freechipsproject](https://github.com/freechipsproject)
* Other available Chisel [IP Contributions](https://github.com/freechipsproject/ip-contributions)
* Yet another AES implementation in Chisel: [github.com/yaozhaosh/chisel-aes](https://github.com/yaozhaosh/chisel-aes)
* Best resource for learning Chisel: [chisel-bootcamp](https://github.com/freechipsproject/chisel-bootcamp)
* Useful and highly recommeded [Scala class on coursera](https://www.coursera.org/learn/progfun1)

## Citation

This work resulted in the folowwing publications:

```
@article{guo2022agile,
  title={Agile-AES: Implementation of configurable AES primitive with agile design approach},
  author={Guo, Xinfei and El-Hadedy, Mohamed and Mosanu, Sergiu and Wei, Xiangdong and Skadron, Kevin and Stan, Mircea R},
  journal={Integration},
  year={2022},
  publisher={Elsevier}
}
@inproceedings{mosanu2019flexi,
  title={Flexi-AES: A Highly-Parameterizable Cipher for a Wide Range of Design Constraints},
  author={Mosanu, Sergiu and Guo, Xinfei and El-Hadedy, Mohamed and Anghel, Lorena and Stan, Mircea},
  booktitle={2019 IEEE 27th Annual International Symposium on Field-Programmable Custom Computing Machines (FCCM)},
  pages={338--338},
  year={2019},
  organization={IEEE}
}
```
