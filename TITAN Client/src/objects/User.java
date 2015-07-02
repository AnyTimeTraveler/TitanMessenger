package objects;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public class User implements Comparable<User>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4150758309873813339L;
	private int ID;
	private String username;
	private String firstname;
	private String lastname;
	private boolean isOnline;
	private Date lastOnline;
	private String status;
	private Date birthDate;
	private String[] emails;
	private String[] phoneNumbers;

	public User(int ID, String username, String firstname, String lastname,
			String[] mailsArray, boolean isOnline, Date lastOnline,
			String status, String[] phonesArray, Date birthDate) {
		this.ID = ID;
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
		this.emails = mailsArray;
		this.isOnline = isOnline;
		this.lastOnline = lastOnline;
		this.status = status;
		this.phoneNumbers = phonesArray;
		this.birthDate = birthDate;
	}

	public int getID() {
		return ID;
	}

	public String getUsername() {
		return username;
	}

	public String getFirstName() {
		return firstname;
	}

	public String getLastName() {
		return lastname;
	}

	public String[] getEmail() {
		return emails;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public Date getLastOnline() {
		return lastOnline;
	}

	public String getStatus() {
		return status;
	}

	public String[] getPhoneNumber() {
		return phoneNumbers;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	@Override
	public int compareTo(User otherUser) {
		if (!(otherUser instanceof User)) {
			throw new ClassCastException("A User object expected.");
		}
		return this.ID - otherUser.ID;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ID;
		result = prime * result
				+ ((birthDate == null) ? 0 : birthDate.hashCode());
		result = prime * result + Arrays.hashCode(emails);
		result = prime * result
				+ ((firstname == null) ? 0 : firstname.hashCode());
		result = prime * result + (isOnline ? 1231 : 1237);
		result = prime * result
				+ ((lastname == null) ? 0 : lastname.hashCode());
		result = prime * result
				+ ((lastOnline == null) ? 0 : lastOnline.hashCode());
		result = prime * result + Arrays.hashCode(phoneNumbers);
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		User other = (User) obj;
		if (ID != other.ID) {
			return false;
		}
		if (birthDate == null) {
			if (other.birthDate != null) {
				return false;
			}
		} else if (!birthDate.equals(other.birthDate)) {
			return false;
		}
		if (!Arrays.equals(emails, other.emails)) {
			return false;
		}
		if (firstname == null) {
			if (other.firstname != null) {
				return false;
			}
		} else if (!firstname.equals(other.firstname)) {
			return false;
		}
		if (isOnline != other.isOnline) {
			return false;
		}
		if (lastname == null) {
			if (other.lastname != null) {
				return false;
			}
		} else if (!lastname.equals(other.lastname)) {
			return false;
		}
		if (lastOnline == null) {
			if (other.lastOnline != null) {
				return false;
			}
		} else if (!lastOnline.equals(other.lastOnline)) {
			return false;
		}
		if (!Arrays.equals(phoneNumbers, other.phoneNumbers)) {
			return false;
		}
		if (status == null) {
			if (other.status != null) {
				return false;
			}
		} else if (!status.equals(other.status)) {
			return false;
		}
		if (username == null) {
			if (other.username != null) {
				return false;
			}
		} else if (!username.equals(other.username)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "User [ID=" + ID + ", username=" + username + ", firstName="
				+ firstname + ", lastName=" + lastname + ", isOnline="
				+ isOnline + ", lastOnline=" + lastOnline + ", status="
				+ status + ", birthDate=" + birthDate + ", emails="
				+ Arrays.toString(emails) + ", phoneNumbers="
				+ Arrays.toString(phoneNumbers) + "]";
	}

}
