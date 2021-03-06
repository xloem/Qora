package qora.block;

import java.util.Arrays;

import qora.account.PublicKeyAccount;

import com.google.common.primitives.Ints;

public class BlockFactory {

	private static BlockFactory instance;
	
	public static BlockFactory getInstance()
	{
		if(instance == null)
		{
			instance = new BlockFactory();
		}
		
		return instance;
	}
	
	private BlockFactory()
	{
		
	}
	
	public Block parse(byte[] data) throws Exception
	{
		//GET HASH
		int position = Block.VERSION_LENGTH + Block.REFERENCE_LENGTH + Block.TIMESTAMP_LENGTH + Block.GENERATING_BALANCE_LENGTH + Block.GENERATOR_LENGTH;
		byte[] signature =  Arrays.copyOfRange(data, position, position + Block.GENERATOR_SIGNATURE_LENGTH);
		
		//CHECK IF GENESISBLOCK
		if(Arrays.equals(GenesisBlock.generateHash(), signature))
		{
			//PARSE GENESISBLOCK
			return new GenesisBlock();
		}
		else
		{
			//PARSE HEIGHT
			byte[] versionBytes =  Arrays.copyOfRange(data, 0, 4);
			int version = Ints.fromByteArray(versionBytes);
			
			//PARSE NORMAL BLOCK
			return ( version > 1 ) ? Block.parse(data) : Block.parseOld(data);
		}
	}

	public Block create(int version, byte[] reference, long timestamp, long baseTarget, PublicKeyAccount generator, byte[] signature, byte[] atBytes, long atFees) 
	{		
		return new Block(version, reference, timestamp, baseTarget, generator, signature, atBytes, atFees);		
	}
	
	public Block create(int version, byte[] reference, long timestamp, long baseTarget, PublicKeyAccount generator, byte[] signature) 
	{		
		return new Block(version, reference, timestamp, baseTarget, generator, signature);		
	}
	
}
