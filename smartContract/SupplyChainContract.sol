pragma solidity ^0.4.25;

contract SupplyChainContract {
    
 //合约主持人(银行)
 address public bank;
 
 //制造商地址
 address private factory;
 
 //记录一笔供应链交易
 struct SupplyTransaction {
  string transNo;   //交易凭单编号
  string transMemo; //交易说明
  address supplier; //供货商
  uint transTime;   //交易时间
  uint transValue;  //实体交易金额
  uint loanTime;    //放款时间
  uint loanValue;   //放款金额
  bool exist;
 }
    
 //存储所有供应链交易
 mapping(uint => SupplyTransaction) public transData;
    
 //总供应链交易数
 uint public transCnt;
    
 //添加供应链交易事件
 event InsTransEvt(string indexed eventType, uint transCnt);
 
 //记录合约主持人(银行)
 constructor () public {
   bank = msg.sender;
 }
 	
 //只有银行可执行 
 modifier onlyBank() {
   require(msg.sender == bank,
   "only bank can do this");
    _;
 }
 
 //只有制造商可执行 
 modifier onlyFactory() {
   require(msg.sender == factory,
   "only factory can do this");
    _;
 }
 
 //智能合约储值
 function () public payable onlyBank{
 }
 
 //查询智能合约余额
 function queryBalance() public view onlyBank returns(uint){
  return address(this).balance;
 }
 
 //设置制造商地址  
 function setFactory(address _factory) public onlyBank {
  factory = _factory;
 }
	
 //查询制造商地址  
 function queryFactory() public view returns(address) {
  return factory;
 }
 
 //添加一笔供应链交易
 function insSupplyTrans(string transNo,string transMemo, address supplier,uint transValue) public onlyFactory returns(uint){
   //供应链交易数量加1
   transCnt++;
         
   transData[transCnt].transNo = transNo; 
   transData[transCnt].transMemo = transMemo;   
   transData[transCnt].supplier = supplier;
   transData[transCnt].transValue = transValue;
   transData[transCnt].transTime = now;
   transData[transCnt].loanTime = 0;
   transData[transCnt].loanValue = 0;   
   transData[transCnt].exist = true;
  
   //触发添加交易事件
   emit InsTransEvt("TransIns", transCnt);
   
   return transCnt;
 }
 
 //查询交易是否存在
 function isTransExist(uint transKey) public view returns(bool) {
   return transData[transKey].exist;
 }
 
 //传输数字加密货币给供货商
 function loanEth(uint transKey, uint loanValue) public onlyBank{
   require(transData[transKey].exist, 
		 "transaction not exist");
   
   //设置放款金额
   transData[transCnt].loanValue = loanValue;
   
   //设置放款时间
   transData[transCnt].loanTime = now;
   
   //指定放款金额转账给供货商
   transData[transCnt].supplier.transfer(loanValue);
 }
}