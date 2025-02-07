c:\jdk1.8\bin\java -cp .;.\lib\* org.web3j.codegen.SolidityFunctionWrapperGenerator ./SupplyChainContract.bin ./SupplyChainContract.abi -o ./java -p chap06.com.alc

rem geth attach ipc:\\.\pipe\geth.ipc
rem loadScript("SupplyChainContract.js");