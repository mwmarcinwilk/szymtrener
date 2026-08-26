package pl.szymtrener.admin;

public class PasswordForm {
    private String current = "";
    private String fresh = "";
    private String repeat = "";

    public String getCurrent() { return current; }
    public void setCurrent(String current) { this.current = current; }
    public String getFresh() { return fresh; }
    public void setFresh(String fresh) { this.fresh = fresh; }
    public String getRepeat() { return repeat; }
    public void setRepeat(String repeat) { this.repeat = repeat; }
}
