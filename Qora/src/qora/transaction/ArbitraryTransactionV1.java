package qora.transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntp.NTP;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import qora.account.Account;
import qora.account.PrivateKeyAccount;
import qora.account.PublicKeyAccount;
import qora.crypto.Base58;
import qora.crypto.Crypto;
import qora.naming.Name;
import qora.payment.Payment;
import qora.web.blog.BlogEntry;
import utils.BlogUtils;
import utils.StorageUtils;
import api.BlogPostResource;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import database.DBSet;

public class ArbitraryTransactionV1 extends ArbitraryTransaction {
	protected static final int CREATOR_LENGTH = 32;
	protected static final int SERVICE_LENGTH = 4;
	protected static final int DATA_SIZE_LENGTH = 4;
	protected static final int REFERENCE_LENGTH = 64;
	protected static final int FEE_LENGTH = 8;
	protected static final int SIGNATURE_LENGTH = 64;
	protected static final int BASE_LENGTH = TIMESTAMP_LENGTH
			+ REFERENCE_LENGTH + CREATOR_LENGTH + SERVICE_LENGTH
			+ DATA_SIZE_LENGTH + FEE_LENGTH + SIGNATURE_LENGTH;

	private PublicKeyAccount creator;
	private int service;
	private byte[] data;

	public ArbitraryTransactionV1(PublicKeyAccount creator, int service,
			byte[] data, BigDecimal fee, long timestamp, byte[] reference,
			byte[] signature) {
		super(fee, timestamp, reference, signature);

		this.service = service;
		this.data = data;
		this.creator = creator;
	}

	// GETTERS/SETTERS

	@Override
	public int getService() {
		return this.service;
	}

	@Override
	public byte[] getData() {
		return this.data;
	}

	// PARSE CONVERT

	public static Transaction Parse(byte[] data) throws Exception {
		// CHECK IF WE MATCH BLOCK LENGTH
		if (data.length < BASE_LENGTH) {
			throw new Exception("Data does not match block length");
		}

		int position = 0;

		// READ TIMESTAMP
		byte[] timestampBytes = Arrays.copyOfRange(data, position, position
				+ TIMESTAMP_LENGTH);
		long timestamp = Longs.fromByteArray(timestampBytes);
		position += TIMESTAMP_LENGTH;

		// READ REFERENCE
		byte[] reference = Arrays.copyOfRange(data, position, position
				+ REFERENCE_LENGTH);
		position += REFERENCE_LENGTH;

		// READ CREATOR
		byte[] creatorBytes = Arrays.copyOfRange(data, position, position
				+ CREATOR_LENGTH);
		PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
		position += CREATOR_LENGTH;

		// READ SERVICE
		byte[] serviceBytes = Arrays.copyOfRange(data, position, position
				+ SERVICE_LENGTH);
		int service = Ints.fromByteArray(serviceBytes);
		position += SERVICE_LENGTH;

		// READ DATA SIZE
		byte[] dataSizeBytes = Arrays.copyOfRange(data, position, position
				+ DATA_SIZE_LENGTH);
		int dataSize = Ints.fromByteArray(dataSizeBytes);
		position += DATA_SIZE_LENGTH;

		// READ DATA
		byte[] arbitraryData = Arrays.copyOfRange(data, position, position
				+ dataSize);
		position += dataSize;

		// READ FEE
		byte[] feeBytes = Arrays.copyOfRange(data, position, position
				+ FEE_LENGTH);
		BigDecimal fee = new BigDecimal(new BigInteger(feeBytes), 8);
		position += FEE_LENGTH;

		// READ SIGNATURE
		byte[] signatureBytes = Arrays.copyOfRange(data, position, position
				+ SIGNATURE_LENGTH);

		return new ArbitraryTransactionV1(creator, service, arbitraryData, fee,
				timestamp, reference, signatureBytes);
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson() {
		// GET BASE
		JSONObject transaction = this.getJsonBase();

		// ADD CREATOR/SERVICE/DATA
		transaction.put("creator", this.creator.getAddress());
		transaction.put("service", this.service);
		transaction.put("data", Base58.encode(this.data));

		return transaction;
	}

	@Override
	public byte[] toBytes() {
		byte[] data = new byte[0];

		// WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);

		// WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH,
				0);
		data = Bytes.concat(data, timestampBytes);

		// WRITE REFERENCE
		data = Bytes.concat(data, this.reference);

		// WRITE CREATOR
		data = Bytes.concat(data, this.creator.getPublicKey());

		// WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(this.service);
		data = Bytes.concat(data, serviceBytes);

		// WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(this.data.length);
		data = Bytes.concat(data, dataSizeBytes);

		// WRITE DATA
		data = Bytes.concat(data, this.data);

		// WRITE FEE
		byte[] feeBytes = this.fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);

		// SIGNATURE
		data = Bytes.concat(data, this.signature);

		return data;
	}

	@Override
	public int getDataLength() {
		return TYPE_LENGTH + BASE_LENGTH + this.data.length;
	}

	// VALIDATE

	@Override
	public boolean isSignatureValid() {
		byte[] data = new byte[0];

		// WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);

		// WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH,
				0);
		data = Bytes.concat(data, timestampBytes);

		// WRITE REFERENCE
		data = Bytes.concat(data, this.reference);

		// WRITE CREATOR
		data = Bytes.concat(data, this.creator.getPublicKey());

		// WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(this.service);
		data = Bytes.concat(data, serviceBytes);

		// WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(this.data.length);
		data = Bytes.concat(data, dataSizeBytes);

		// WRITE DATA
		data = Bytes.concat(data, this.data);

		// WRITE FEE
		byte[] feeBytes = this.fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);

		return Crypto.getInstance().verify(this.creator.getPublicKey(),
				this.signature, data);
	}

	@Override
	public int isValid(DBSet db) {
		// CHECK IF RELEASED
		if (NTP.getTime() < ARBITRARY_TRANSACTIONS_RELEASE) {
			return NOT_YET_RELEASED;
		}

		// CHECK DATA SIZE
		if (data.length > 4000 || data.length < 1) {
			return INVALID_DATA_LENGTH;
		}

		// CHECK IF CREATOR HAS ENOUGH MONEY
		if (this.creator.getBalance(1, db).compareTo(this.fee) == -1) {
			return NO_BALANCE;
		}

		// CHECK IF REFERENCE IS OKE
		if (!Arrays.equals(this.creator.getLastReference(db), this.reference)) {
			return INVALID_REFERENCE;
		}

		// CHECK IF FEE IS POSITIVE
		if (this.fee.compareTo(BigDecimal.ZERO) <= 0) {
			return NEGATIVE_FEE;
		}

		return VALIDATE_OKE;
	}

	// PROCESS/ORPHAN
	@Override
	public void process(DBSet db) {

		try {
			// NAME STORAGE UPDATE
			if (service == 10) {
				StorageUtils.processUpdate(getData(), signature, creator,
						DBSet.getInstance());
				StorageUtils.processUpdate(getData(), signature, creator, db);
				// BLOGPOST?
			} else if (service == 777) {
				addToBlogMapOnDemand(DBSet.getInstance());
				addToBlogMapOnDemand(db);
			} else if (service == BlogUtils.COMMENT_SERVICE_ID) {
				addToCommentMapOnDemand(DBSet.getInstance());
				addToCommentMapOnDemand(db);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// UPDATE CREATOR
		this.creator.setConfirmedBalance(this.creator.getConfirmedBalance(db)
				.subtract(this.fee), db);

		// UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.signature, db);
	}

	@Override
	public void orphan(DBSet db) {

		// NAME STORAGE UPDATE ORPHAN
		// if (service == 10) {
		// StorageUtils.processOrphan(getData(), signature, db);
		// // BLOGPOST?
		// } else {
		// removeFromBlogMapOnDemand(db);
		// }

		// UPDATE CREATOR
		this.creator.setConfirmedBalance(this.creator.getConfirmedBalance(db)
				.add(this.fee), db);

		// UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.reference, db);
	}

	@Override
	public PublicKeyAccount getCreator() {
		return this.creator;
	}

	@Override
	public List<Account> getInvolvedAccounts() {
		List<Account> accounts = new ArrayList<Account>();

		accounts.add(this.creator);

		return accounts;
	}

	@Override
	public boolean isInvolved(Account account) {
		String address = account.getAddress();

		if (address.equals(this.creator.getAddress())) {
			return true;
		}

		return false;
	}

	@Override
	public BigDecimal getAmount(Account account) {
		String address = account.getAddress();

		if (address.equals(this.creator.getAddress())) {
			return BigDecimal.ZERO.setScale(8).subtract(this.fee);
		}

		return BigDecimal.ZERO.setScale(8);
	}

	public static byte[] generateSignature(DBSet db, PrivateKeyAccount creator,
			int service, byte[] arbitraryData, BigDecimal fee, long timestamp) {
		byte[] data = new byte[0];

		// WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(ARBITRARY_TRANSACTION);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);

		// WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH,
				0);
		data = Bytes.concat(data, timestampBytes);

		// WRITE REFERENCE
		data = Bytes.concat(data, creator.getLastReference(db));

		// WRITE CREATOR
		data = Bytes.concat(data, creator.getPublicKey());

		// WRITE SERVICE
		byte[] serviceBytes = Ints.toByteArray(service);
		data = Bytes.concat(data, serviceBytes);

		// WRITE DATA SIZE
		byte[] dataSizeBytes = Ints.toByteArray(arbitraryData.length);
		data = Bytes.concat(data, dataSizeBytes);

		// WRITE DATA
		data = Bytes.concat(data, arbitraryData);

		// WRITE FEE
		byte[] feeBytes = fee.unscaledValue().toByteArray();
		byte[] fill = new byte[FEE_LENGTH - feeBytes.length];
		feeBytes = Bytes.concat(fill, feeBytes);
		data = Bytes.concat(data, feeBytes);

		return Crypto.getInstance().sign(creator, data);
	}

	@Override
	public void addToCommentMapOnDemand(DBSet db) {

		if (getService() == BlogUtils.COMMENT_SERVICE_ID) {
			byte[] data = getData();
			String string = new String(data, Charsets.UTF_8);

			JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
			if (jsonObject != null) {
				
				String signatureOfCommentOpt = (String) jsonObject
						.get(BlogPostResource.DELETE_KEY);
				
				//CHECK IF THIS IS A DELETE OR CREATE OF A COMMENT
				if(StringUtils.isNotBlank(signatureOfCommentOpt))
				{
					BlogEntry commentEntryOpt = BlogUtils.getCommentBlogEntryOpt(signatureOfCommentOpt);
					
					String authorOpt = (String) jsonObject
							.get(BlogPostResource.AUTHOR);

					if (commentEntryOpt != null) {
						String creatorOfDeleteTX = getCreator().getAddress();
						String creatorOfEntryToDelete = commentEntryOpt
								.getCreator();

							// OWNER IS DELETING OWN POST?
							if (creatorOfDeleteTX
									.equals(creatorOfEntryToDelete)) {
								deleteCommentInternal(db, commentEntryOpt);
								// BLOGOWNER IS DELETING POST
							} else if (authorOpt != null
									&& commentEntryOpt.getBlognameOpt() != null) {
								Name name = db.getNameMap().get(
										commentEntryOpt.getBlognameOpt());
								if (name != null
										&& name.getOwner().getAddress()
												.equals(creatorOfDeleteTX)) {
									deleteCommentInternal(db, commentEntryOpt);
									
								}
							}

					}
				}else
				{
					String post = (String) jsonObject
							.get(BlogPostResource.POST_KEY);
					
					String postid = (String) jsonObject
							.get(BlogPostResource.COMMENT_POSTID_KEY);
					
					// DOES POST MET MINIMUM CRITERIUM?
					if (StringUtils.isNotBlank(post)
							&& StringUtils.isNotBlank(postid)) {
						
						db.getPostCommentMap().add(Base58.decode(postid),
								getSignature());
						db.getCommentPostMap().add(getSignature(), Base58.decode(postid));
					}
				}
				
				
			}

		}

	}

	private void addToBlogMapOnDemand(DBSet db) {

		if (getService() == 777) {
			byte[] data = getData();
			String string = new String(data, Charsets.UTF_8);

			JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
			if (jsonObject != null) {
				String post = (String) jsonObject
						.get(BlogPostResource.POST_KEY);

				String blognameOpt = (String) jsonObject
						.get(BlogPostResource.BLOGNAME_KEY);

				String share = (String) jsonObject
						.get(BlogPostResource.SHARE_KEY);

				String delete = (String) jsonObject
						.get(BlogPostResource.DELETE_KEY);

				String author = (String) jsonObject
						.get(BlogPostResource.AUTHOR);

				boolean isShare = false;
				if (StringUtils.isNotEmpty(share)) {
					isShare = true;
					byte[] sharedSignature = Base58.decode(share);
					if (sharedSignature != null) {
						db.getSharedPostsMap().add(sharedSignature, author);
					}
				}

				if (StringUtils.isNotEmpty(delete)) {
					BlogEntry blogEntryOpt = BlogUtils.getBlogEntryOpt(delete);

					if (blogEntryOpt != null) {
						String creatorOfDeleteTX = getCreator().getAddress();
						String creatorOfEntryToDelete = blogEntryOpt
								.getCreator();
						if (blogEntryOpt != null) {

							// OWNER IS DELETING OWN POST?
							if (creatorOfDeleteTX
									.equals(creatorOfEntryToDelete)) {
								deleteInternal(db, isShare, blogEntryOpt);
								// BLOGOWNER IS DELETING POST
							} else if (author != null
									&& blogEntryOpt.getBlognameOpt() != null) {
								Name name = db.getNameMap().get(
										blogEntryOpt.getBlognameOpt());
								if (name != null
										&& name.getOwner().getAddress()
												.equals(creatorOfDeleteTX)) {
									deleteInternal(db, isShare, blogEntryOpt);
								}
							}

						}
					}

				} else {

					// DOES POST MET MINIMUM CRITERIUM?
					if (StringUtils.isNotBlank(post)) {

						// Shares won't be hashtagged!
						if (!isShare) {
							List<String> hashTags = BlogUtils.getHashTags(post);
							for (String hashTag : hashTags) {
								db.getHashtagPostMap().add(hashTag,
										getSignature());
							}
						}

						db.getBlogPostMap().add(blognameOpt, getSignature());
					}
				}

			}
		}
	}

	@Override
	public void deleteInternal(DBSet db, boolean isShare, BlogEntry blogEntryOpt) {
		if (isShare) {
			byte[] sharesignature = Base58.decode(blogEntryOpt
					.getShareSignatureOpt());
			db.getBlogPostMap().remove(blogEntryOpt.getBlognameOpt(),
					sharesignature);
			db.getSharedPostsMap().remove(sharesignature,
					blogEntryOpt.getNameOpt());
		} else {
			// removing from hashtagmap
			List<String> hashTags = BlogUtils.getHashTags(blogEntryOpt
					.getDescription());
			for (String hashTag : hashTags) {
				db.getHashtagPostMap().remove(hashTag,
						Base58.decode(blogEntryOpt.getSignature()));
			}
			db.getBlogPostMap().remove(blogEntryOpt.getBlognameOpt(),
					Base58.decode(blogEntryOpt.getSignature()));
		}
	}
	
	@Override
	public void deleteCommentInternal(DBSet db, BlogEntry commentEntry) {
		
		byte[] signatureOfComment = Base58.decode(commentEntry.getSignature());
			byte[] signatureOfBlogPostOpt = db.getCommentPostMap().get(Base58.decode(commentEntry.getSignature()));
			// removing from hashtagmap
			
			if(signatureOfBlogPostOpt != null)
			{
				db.getPostCommentMap().remove(signatureOfBlogPostOpt, signatureOfComment);
				db.getCommentPostMap().remove(signatureOfComment);
				
			}
	}

	// TODO implement readd delete if orphaned!
	@SuppressWarnings("unused")
	private void removeFromBlogMapOnDemand(DBSet db) {
		if (getService() == 777) {
			byte[] data = getData();
			String string = new String(data, Charsets.UTF_8);

			JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
			if (jsonObject != null) {
				String blognameOpt = (String) jsonObject
						.get(BlogPostResource.BLOGNAME_KEY);

				String share = (String) jsonObject
						.get(BlogPostResource.SHARE_KEY);

				String author = (String) jsonObject
						.get(BlogPostResource.AUTHOR);

				if (StringUtils.isNotEmpty(share)) {
					byte[] sharedSignature = Base58.decode(share);
					if (sharedSignature != null) {
						db.getSharedPostsMap().remove(sharedSignature, author);
					}
				}

				db.getBlogPostMap().remove(blognameOpt, getSignature());

			}
		}
	}

	@Override
	public List<Payment> getPayments() {
		return new ArrayList<Payment>();
	}
}