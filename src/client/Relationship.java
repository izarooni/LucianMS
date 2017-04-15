package client;

/**
 * @author izarooni
 */
public class Relationship {

    public enum Status {
        Single, Engaged, Married
    }

    private Status status = Status.Single;
    private String partnerName = null;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }
}
