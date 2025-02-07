package chap06.sec04;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple8;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import chap06.com.alc.SupplyChainContract;
import rx.Subscription;
import rx.functions.Action1;

public class SupplyChainBank {

	public static void main(String[] args) {
		new SupplyChainBank();
	}

	// 区块链节点地址
	private static String blockchainNode = "http://127.0.0.1:8080/";

	// 智能合约地址
	private static String contractAddr = "0x069ce65305532f6e125366a9f98b90de511ff4e1";

	// 银行密钥文件
	private String bankKey = "C:\\MyGeth\\node01\\keystore\\UTC--2018-05-12T05-36-09.868221900Z--4cd063815f7f7a26504ae42a3693b4bbdf0b9b1a";

	// 银行EOA
	String bank = "0x4cd063815f7f7a26504ae42a3693b4bbdf0b9b1a";

	// 制造商密钥文件
	private String factoryKey = "C:\\MyGeth\\node01\\keystore\\UTC--2018-11-25T03-41-09.743521200Z--576b11fb5d5c380fcf973b62c3ab59f19f9300fe";

	// 制造商EOA
	String factory = "0x576B11Fb5D5C380fCF973b62C3aB59f19f9300fE";

	//供货商EOA
	String supplier = "0xDa85610910365341D3372fa350F865Ce50224a91";
	
	public SupplyChainBank() {
		// step1. 银行设置制造商
		initFactory(bankKey, "16888", factory);

		// step2. 余额存放在智能合约中
		transferETH(bank, contractAddr, bankKey, "16888", "200");

		// step3. 银行进行事件监听
		// step4. 监听事件后，获取交易信息
		// step5. 进行放款的操作
		startOracle(contractAddr);

		// step6. 供货商确认拨款
	}

	// 设置制造商
	private void initFactory(String keyFile, String myPWD, String factory) {
		try {
			// 连接区块链节点
			Web3j web3 = Web3j.build(new HttpService(blockchainNode));

			// 指定密钥文件并进行账号和密码的验证
			Credentials credentials = WalletUtils.loadCredentials(myPWD, keyFile);

			// 获取合约封装对象
			SupplyChainContract contract = SupplyChainContract.load(contractAddr, web3, credentials,
					SupplyChainContract.GAS_PRICE, SupplyChainContract.GAS_LIMIT);

			// 设置制造商地址
			contract.setFactory(factory).send();
			System.out.println("设置制造商，完成");

		} catch (Exception e) {
			System.out.println("设置制造商错误，错误：" + e);
		}
	}

	// 转账以太币
	private void transferETH(String fromEOA, String toEOA, String keyFile, String pwd, String eth) {
		try {
			// 连接区块链节点
			Web3j web3 = Web3j.build(new HttpService(blockchainNode));

			// 验证签名对象
			Credentials credentials = WalletUtils.loadCredentials(pwd, keyFile);

			// 设置ETH数量
			BigInteger ethValue = Convert.toWei(eth, Convert.Unit.ETHER).toBigInteger();

			// 设置nonce随机数
			EthGetTransactionCount ethGetTransactionCount = web3
					.ethGetTransactionCount(fromEOA, DefaultBlockParameterName.LATEST).sendAsync().get();
			BigInteger nonce = ethGetTransactionCount.getTransactionCount();

			// 设置燃料Gas
			BigInteger gasPrice = new BigInteger("" + 1);
			BigInteger gasLimit = new BigInteger("" + 30000);

			// 创建RawTransaction对象
			RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toEOA,
					ethValue);

			// 对交易进行签名与加密
			byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
			String hexValue = Numeric.toHexString(signedMessage);

			// 传送交易
			EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get();

			String txnHash = ethSendTransaction.getTransactionHash();
			System.out.println("传送以太币交易序号：" + txnHash);

		} catch (Exception e) {
			System.out.println("transferETH，错误：" + e);
		}
	}

	// 启动Oracle服务
	public void startOracle(String contractAddr) {
		try {
			// 连接区块链节点

			Admin web3 = Admin.build(new HttpService(blockchainNode));

			// 设置过滤条件
			EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST,
					contractAddr);

			// 获取事件topic的哈希编码
			String eventTopicHash = Hash.sha3String("TransIns");

			// 交易事件的日志（Log）
			Function transLog = new Function("", Collections.<Type>emptyList(),
					Arrays.asList(new TypeReference<Uint>() {
					}));

			// 持续监听事件
			Subscription subscription = web3.ethLogObservable(filter).subscribe(new Action1<Log>() {
				public void call(Log log) {
					List<String> list = log.getTopics();
					// 遍历事件中的Topic
					for (String topic : list) {
						if (topic.equals(eventTopicHash)) {
							System.out.println("处理交易事件");
							handleTransEvent(log, transLog);
						}
					}
				}
			});
		} catch (Exception e) {
			System.out.println("Oracle监听错误：" + e);
		}
	}

	// 处理交易事件
	private void handleTransEvent(Log log, Function function) {
		try {
			List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), function.getOutputParameters());
			int inx = 0;
			long transKey = 0l; // 交易ID
			for (Type type : nonIndexedValues) {
				System.out.println("Type String:" + type.getTypeAsString());
				System.out.println("Type Value:" + type.getValue());
				if (inx == 0) {
					// 返回的参数是交易编号
					try {
						transKey = ((BigInteger) type.getValue()).longValue();
					} catch (Exception e) {
						System.out.println("convert error:" + e);
					}
				}
				inx++;
			}

			// 判断供应链交易是否存在
			Long transValueObj = querySupplyChainTrans(bankKey, "16888", transKey);
			if (transValueObj != null && transValueObj.longValue() > 0) {
				// 执行放款
				System.out.println("准备进行放款");
				executeLoan(bankKey, "16888", transKey, transValueObj.longValue());
			} else {
				// 不执行放款
				System.out.println("交易不存在，不进行放款");
			}

		} catch (Exception e) {
			System.out.println("Error:" + e);
		}
	}

	// 查询供应链交易
	private Long querySupplyChainTrans(String keyFile, String myPWD, long transKey) {
		Long transValueObj = null;
		try {
			// 连接区块链节点
			Web3j web3 = Web3j.build(new HttpService(blockchainNode));

			// 指定密钥文件并进行账号和密码的验证
			Credentials credentials = WalletUtils.loadCredentials(myPWD, keyFile);

			// 获取合约封装对象
			SupplyChainContract contract = SupplyChainContract.load(contractAddr, web3, credentials,
					SupplyChainContract.GAS_PRICE, SupplyChainContract.GAS_LIMIT);

			// 查询交易是否存在
			if (contract.isTransExist(new BigInteger("" + transKey)).send()) {
				System.out.println("供应链交易存在");

				// 获取交易对象
				Tuple8 transData = contract.transData(new BigInteger("" + transKey)).send();

				// 交易凭单编号
				String transNo = (String) transData.getValue1();

				// 交易说明
				String transMemo = (String) transData.getValue2();

				// 供货商
				String supplier = (String) transData.getValue3();

				// 交易时间
				BigInteger transTime = (BigInteger) transData.getValue4();

				// 实体交易金额
				BigInteger transValue = (BigInteger) transData.getValue5();
				transValueObj = transValue.longValue();

				// 放款时间
				BigInteger loanTime = (BigInteger) transData.getValue6();

				// 放款金额
				BigInteger loanValue = (BigInteger) transData.getValue7();

				// 交易存在旗标
				Boolean exist = (Boolean) transData.getValue8();

				// 时间显示格式
				SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				Calendar bolckTimeCal = Calendar.getInstance();

				System.out.println("交易凭单编号：" + transNo);
				System.out.println("交易说明：" + transMemo);
				System.out.println("供货商：" + supplier);

				bolckTimeCal.setTimeInMillis(transTime.longValueExact() * 1000);
				System.out.println("交易时间：" + timeFormat.format(bolckTimeCal.getTime()));

				System.out.println("实体交易金额：" + transValue);

				bolckTimeCal.setTimeInMillis(loanTime.longValueExact() * 1000);
				System.out.println("放款时间：" + timeFormat.format(bolckTimeCal.getTime()));

				System.out.println("放款金额：" + loanValue.longValue());
				System.out.println("交易存在标志：" + exist);
			} else {
				System.out.println("供应链交易不存在");
				transValueObj = null;
			}

		} catch (Exception e) {
			System.out.println("查询供应链交易错误，错误：" + e);
		}
		return transValueObj;
	}

	// 执行放款
	private void executeLoan(String keyFile, String myPWD, long transKey, long transValue) {
		try {
			// 连接区块链节点
			Web3j web3 = Web3j.build(new HttpService(blockchainNode));

			// 指定密钥文件并进行账号和密码的验证
			Credentials credentials = WalletUtils.loadCredentials(myPWD, keyFile);

			// 获取合约封装对象
			SupplyChainContract contract = SupplyChainContract.load(contractAddr, web3, credentials,
					SupplyChainContract.GAS_PRICE, SupplyChainContract.GAS_LIMIT);

			// 查询交易是否存在
			Boolean isExist = contract.isTransExist(new BigInteger("" + transKey)).send();
			if (isExist) {
				System.out.println("供应链交易存在，准备进行放款");

				// 计算放款额度
				long loanValue = transValue / 10;
				BigInteger weiValue = Convert.toWei("" + loanValue, Convert.Unit.ETHER).toBigInteger();
				
				// 执行放款
				contract.loanEth(new BigInteger("" + transKey), new BigInteger("" + weiValue)).send();
				System.out.println("完成放款");

			} else {
				System.out.println("供应链交易不存在");
			}

		} catch (Exception e) {
			System.out.println("执行放款错误，错误：" + e);
		}
	}
}