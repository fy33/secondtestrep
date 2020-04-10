package com.yuan.test.test;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.xml.sax.EntityResolver;

import java.util.Map;

public interface MyDocument  extends Branch {
    Element getRootElement();
    void setRootElement(Element rootElement);
    Document addComment(String comment);
    Document addProcessingInstruction(String target,String text);
    Document addProcessingInstruction(String target, Map data);
    Document addDocType(String name,String publicId,String systemId);

    DocumentType getDocType();
    void setDocType(DocumentType docType);
    EntityResolver getEntityResolver();
    void setEntityResolver(EntityResolver entityResolver);
    String getXMLEncoding();
    void setXMLEncoding(String encoding);

}
