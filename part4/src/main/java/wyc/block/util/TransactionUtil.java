package wyc.block.util;

import org.apache.log4j.Logger;
import wyc.block.constant.BlockConstant;
import wyc.block.entity.Block;
import wyc.block.entity.Transaction;
import wyc.block.entity.TxInput;
import wyc.block.entity.TxOutput;

import java.util.*;

public class TransactionUtil {

    private static Logger logger = Logger.getLogger(TransactionUtil.class);

    private static final int subsidy = 10;//奖励的数额

    /**
     * 判断是否是 coinbase 交易
     * @param tx
     * @return
     */
    public static  boolean isCoinbase(Transaction tx){
        return tx.getvIns().size()==1 && tx.getvIns().get(0).getTxId().length==0 && tx.getvIns().get(0).getVout()==-1;
    }

    /**
     *  获取一个 coinbase 交易，该没有输入，只有一个输出
     * @param data
     * @param to
     * @return
     */
    public static Transaction getNewCoinbaseTx(String data,String to){
        if(data ==""){
            data=to;
        }
        List vIns = new ArrayList<TxInput>();
        vIns.add(new TxInput(new byte[0],-1,data));

        List vOuts = new ArrayList<TxOutput>();
        vOuts.add( new TxOutput(subsidy,to));

        return new  Transaction(vIns,vOuts);
    }

    /**
     * 获取一笔新的交易
     * @param from
     * @param to
     * @param amount
     * @return
     */
    public static Transaction getNewUTXOTransaction(String from,String to,int amount){
        List<TxInput> txInputs = new ArrayList<TxInput>();
        List<TxOutput> txOutPuts = new ArrayList<TxOutput>();
        Map data = findSpendableOutputs(from,amount);
        int acc = Integer.valueOf(data.get("accumulated").toString());
        // 找到足够的未花费输出
        Map<String ,List<Integer>> unspentOutputs = (Map<String ,List<Integer>>)data.get("unspentOutputs");
        if(acc < amount){
            logger.error("ERROR: Not enough funds");
            System.exit(1);
        }
        for (Map.Entry<String,List<Integer>> entry : unspentOutputs.entrySet()) {
            byte[] txID = DataUtil.string2Bytes(entry.getKey());
            for(int out : entry.getValue() ){
                txInputs.add(new TxInput(txID,out,from));
            }
        }
        txOutPuts.add(new TxOutput(amount,to));

        // 如果 UTXO 总数超过所需，则产生找零
        if (acc >amount){
            txOutPuts.add(new TxOutput(acc - amount,from));
        }
        return new Transaction(txInputs,txOutPuts);
    }

    /**
     * 从链中找到未花费输出的交易
     * (指输出还没有被包含在任何交易的输入中)
     * @return
     */
    public static List<Transaction> findUnspentTransactions(String address){
        List<Transaction> unspentTxs = new ArrayList<Transaction>();
        Map<String ,List<Integer>> spentTXOs = new HashMap<String ,List<Integer>>();
        Object lastHashO = RedisUtil.get(BlockConstant.LAST_HASH_INDEX,BlockConstant.LAST_HASH_KEY);
        if(lastHashO!=null){
            byte[] blockHash = (byte[])lastHashO;
            while(blockHash.length!=0){//遍历所有区块
                Block block = BlockUtil.deserialize(RedisUtil.get(BlockConstant.BLOCK_INDEX,blockHash));
                List<Transaction> txs = block.getTransactions();

                for(Transaction tx : txs) {//遍历区块中所有交易
                    String txId = DataUtil.byte2Hex(tx.getId());
                    Outputs:
                    for(int i=0;i< tx.getvOuts().size();i++){
                        if(spentTXOs.get(txId)!=null){
                            for(int spentOut:spentTXOs.get(txId)){
                                if(spentOut == i){
                                    continue Outputs;
                                }
                            }
                        }
                        if (tx.getvOuts().get(i).canBeUnlockedWith(address)) {
                            unspentTxs.add(tx);
                        }
                    }

                    if(!isCoinbase(tx)){
                        for(TxInput in :tx.getvIns()){
                            if(in.canUnlockOutPutWith(address)){
                                String inTxId = DataUtil.bytes2String(in.getTxId());
                                if( spentTXOs.get(inTxId)==null){
                                    spentTXOs.put(inTxId,new ArrayList<Integer>());
                                }
                                spentTXOs.get(inTxId).add(in.getVout());
                            }
                        }
                    }
                }
                blockHash = block.getPrevBlockHash();
            }

        }else{
            logger.info("No existing blockchain found. Create one first!");
            System.exit(1);
        }

        return unspentTxs;
    }

    /**
     * 找到未花费输出
     * @param address
     * @return
     */
    public static List<TxOutput> findUTXO(String address){
        List<TxOutput> uTxOs = new ArrayList<TxOutput>();
        List<Transaction> unspentTxs =  findUnspentTransactions(address);

        for(Transaction tx : unspentTxs){
            for(TxOutput txO : tx.getvOuts()){
                if(txO.canBeUnlockedWith(address)){
                    uTxOs.add(txO);
                }
            }
        }
        return uTxOs;
    }

    /**
     * FindSpendableOutputs 从 address 中找到至少有 amount 的 UTXO
     */
    public static Map findSpendableOutputs(String address,int amount){
        Map retMap = new HashMap();
        Map<String ,List<Integer>> unspentOutputs = new HashMap<String ,List<Integer>>();
        List<Transaction> unspentTxs =  findUnspentTransactions(address);
        int accumulated =0;

        for(Transaction unspentTx : unspentTxs){
            String txId = DataUtil.byte2Hex(unspentTx.getId());
            if(unspentOutputs.get(txId)==null){
                unspentOutputs.put(txId,new ArrayList<Integer>());
            }
            for(int i=0;i<unspentTx.getvOuts().size();i++){
                if(unspentTx.getvOuts().get(i).canBeUnlockedWith(address) && accumulated<amount){
                    accumulated += unspentTx.getvOuts().get(i).getValue();
                    unspentOutputs.get(txId).add(i);
                    if (accumulated >= amount){
                        break ;
                    }
                }
            }
        }
        retMap.put("accumulated",accumulated);
        retMap.put("unspentOutputs",unspentOutputs);
        return retMap;
    }

    /**
     * 获取地址账户余额
     * 账户余额就是由账户地址锁定的所有未花费交易输出的总和。
     * @param address
     */
    public static void  getBalance(String address){
        int balance =0;
        List<TxOutput> uTxOs = findUTXO(address);
        for(TxOutput uTxO :uTxOs ){
            balance+=uTxO.getValue();
        }
        logger.info("Balance of "+address+" : "+balance);
    }

    /**
     * 发送
     * @param from 发送地址
     * @param to 目标地址
     * @param amount 发送数量
     */
    public static void send(String from ,String to ,int amount){
        List<Transaction> txs = new ArrayList<Transaction>();
        txs.add(getNewUTXOTransaction(from,to,amount));
        BlockChainUtil.mineBlock(txs);
        logger.info("Send Success!");
    }

}
