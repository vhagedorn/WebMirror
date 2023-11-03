package me.vadim.archive;

/**
 * HTML tags with URL attributes.
 *
 * @link <a href="https://stackoverflow.com/a/2725168/12344841">Adaptation</a>
 * @author vadim
 */
public enum URIElement {

	// HTML4
	a("href"),
	applet("codebase,archive"),
	area("href"),
	base("href"),
	blockquote("cite"),
	body("background"),
	del("cite"),
	form("action"),
	frame("longdesc,src"),
	head("profile"),
	iframe("longdesc,src"),
	img("longdesc,src,usemap"),
	input("src,usemap,formaction"),
	ins("cite"),
	link("href"),
	object("classid,codebase,data,usemap,archive"),
	q("cite"),
	script("src"),

	// HTML5
	audio("src"),
	button("formaction"),
	command("icon"),
	embed("src"),
	html("manifest"),
	source("src"),
	track("src"),
	video("poster,src");

	private final String attr;

	URIElement(String attr) {
		this.attr = attr;
	}

	/**
	 * @return tag name which can hold URL
	 */
	public String getTag() {
		return name();
	}

	/**
	 * @return attributes which may contain one URL
	 */
	public String[] getAttrs() {
		return attr.split(",");
	}

}
