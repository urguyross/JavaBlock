package wyc.block;

import org.apache.log4j.Logger;
import wyc.block.constant.WalletConstant;
import wyc.block.entity.ECDSAKey;
import wyc.block.entity.Wallet;
import wyc.block.util.DataUtil;
import wyc.block.util.blockchain.BlockChainUtil;
import wyc.block.util.blockchain.WalletUtil;
import wyc.block.util.encrypt.ECDSAUtil;
import wyc.block.util.encrypt.SerializeUtil;
import wyc.block.util.transacation.TransactionUtil;

import java.util.HashMap;
import java.util.UUID;

public class BlockMain {

	private static Logger logger = Logger.getLogger(BlockMain.class);

    private static final  String walletFilePath = WalletConstant.WALLET_FILE_DIR+WalletConstant.WALLET_FILE;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws  Exception{

		String address1 = WalletUtil.createWallet();
		String address2 = WalletUtil.createWallet();
		String address3 = WalletUtil.createWallet();
	//	System.out.println("address1:"+address1);
	//	System.out.println("address2:"+address2);
		BlockChainUtil.createBlockChain(address1);
		TransactionUtil.send(address1,address2,1);
		TransactionUtil.send(address1,address3,2);
	//	TransactionUtil.send("1KLxKV3eCNuBTNLSm6JkhKQRNuCBxbSkUw","1GNqYESv3btf3ujiAbbE5XaCELv6Q8qvrz",2);
	//	TransactionUtil.getBalance("1KLxKV3eCNuBTNLSm6JkhKQRNuCBxbSkUw");


		WalletUtil.printWallet();
		BlockChainUtil.printChain();
	}

}
