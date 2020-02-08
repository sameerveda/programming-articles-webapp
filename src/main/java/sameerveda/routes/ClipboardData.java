package sameerveda.routes;

import java.time.LocalDateTime;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName=ClipboardData.TABLE_NAME)
public class ClipboardData {
	public static final String TABLE_NAME = "sameerveda.routes.ClipboardData";
	public static final String ID = "id";
	public static final String STRING_DATA = "string_data";
	
	@DynamoDBHashKey
	String id;
	@DynamoDBAttribute(attributeName=ClipboardData.STRING_DATA)
	String stringData;
	String updatedOn;
	
	public ClipboardData() { }
	
	public ClipboardData(String id, String data) {
		this.id = id;
		this.stringData = data;
		this.updatedOn = LocalDateTime.now().toString();
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUpdatedOn() {
		return updatedOn;
	}
	public void setUpdatedOn(String updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getStringData() {
		return stringData;
	}

	public void setStringData(String stringData) {
		this.stringData = stringData;
	}
	
}
