package chap06.sec04;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.Callback;
import org.web3j.tx.response.QueuingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import chap06.com.alc.SupplyChainContract;

public class SupplyChainFactory {

	public static void main(String[] args) {
		new SupplyChainFactory();
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

	// 供货商EOA
	String supplier = "0xDa85610910365341D3372fa350F865Ce50224a91";

	public SupplyChainFactory() {
		// step1. 上传一笔供应链交易信息
		insSupplyTrans(factoryKey, "16888", "ABC888", "购买网络设备", supplier, 200);
	}

	// 添加供应链交易
	private void insSupplyTrans(String keyFile, String myPWD, String transNo, String transMemo, String supplier,
			long transValue) {
		try {
			// 连接区块链节点
			Web3j web3 = Web3j.build(new HttpService(blockchainNode));

			// 指定密钥文件并进行账号和密码的验证
			Credentials credentials = WalletUtils.loadCredentials(myPWD, keyFile);
			System.out.println("身份验证");

			int attemptsPerTxHash = 30;
			long frequency = 1000;

			// 创建事务处理程序
			TransactionReceiptProcessor myProcessor = new QueuingTransactionReceiptProcessor(web3,
					new InsTransCallBack(), attemptsPerTxHash, frequency);

			// 创建交易管理器
			TransactionManager transactionManager = new RawTransactionManager(web3, credentials, ChainId.NONE,
					myProcessor);
			System.out.println("创建交易管理器");

			// 获取合约封装对象
			SupplyChainContract contract = SupplyChainContract.load(contractAddr, web3, transactionManager,
					SupplyChainContract.GAS_PRICE, SupplyChainContract.GAS_LIMIT);
			System.out.println("获取合约");

			// 添加一笔供应链交易
			contract.insSupplyTrans(transNo, transMemo, supplier, new BigInteger("" + transValue)).sendAsync();			
			System.out.println("添加供应链交易");
		} catch (Exception e) {
			System.out.println("添加供应链交易错误，错误：" + e);
		}
	}	
}

// 处理函数具有整数返回值
class InsTransCallBack implements Callback {
	// 交易被接受的回调函数
	public void accept(TransactionReceipt recp) {

		// 定义函数返回值
		Function function = new Function("", Collections.<Type>emptyList(), Arrays.asList(new TypeReference<Uint>() {
		}));

		// 获取返回值
		List<Log> list = recp.getLogs();
		List<Type> nonIndexedValues = FunctionReturnDecoder.decode(list.get(0).getData(),
				function.getOutputParameters());

		// 第一个返回值是uint
		BigInteger newsKey = (BigInteger) nonIndexedValues.get(0).getValue();
		System.out.println("供应链交易主键：" + newsKey.intValue());
	}

	public void exception(Exception exception) {
		System.out.println("交易失败，err：" + exception);
	}
}