package org.knime.knip.patents.util.nodes.drawings;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author seebacher
 *
 */
public class DrawingsContentHandler implements ContentHandler {

	/*
	 * Fields
	 */
	private boolean thumbnailsStarted = false;
	private int start_page = -1;
	private int number_of_pages = -1;
	private String link = null;

	/*
	 * SAX Methods
	 */

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equalsIgnoreCase("document-instance")) {
			thumbnailsStarted = false;
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		if (localName.equalsIgnoreCase("document-instance")
				&& atts.getValue("desc").equalsIgnoreCase("Drawing")) {
			link = atts.getValue("link");
			number_of_pages = Integer.valueOf(atts.getValue("number-of-pages"));
			thumbnailsStarted = true;
		} else if (thumbnailsStarted
				&& localName.equalsIgnoreCase("document-section")) {
			start_page = Integer.parseInt(atts.getValue("start-page"));
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
	}

	/*
	 * Getter
	 */

	public int getStartPage() {
		return start_page;
	}

	public int getNumberOfPages() {
		return number_of_pages;
	}

	public String getLink() {
		return link;
	}
}