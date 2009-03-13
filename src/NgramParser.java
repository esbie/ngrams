	import java.util.*;
	import javax.xml.parsers.*;
	import org.w3c.dom.*;

public class NgramParser {
		
		public Element rootElement;
		public HashSet<String> samples = new HashSet<String>();

		public NgramParser(String filename){
			//initializing the xml parser
			try{
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(filename);
				rootElement = doc.getDocumentElement();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
		public HashSet<String> parse(){
			NodeList docnodes = rootElement.getElementsByTagName("DOC");
			for(int i = 0; i<docnodes.getLength(); i++){
				Element doc = (Element) docnodes.item(i);
				Element text = (Element) doc.getElementsByTagName("TEXT").item(0);
				Node paragraph = text.getLastChild();
				samples.addAll(Arrays.asList(parseTextNode(paragraph)));
			}
			return samples;
		}

		private String[] parseTextNode(Node node){
			String text = node.getNodeValue();
			text = text.replaceAll("\\.\\s", "\\.\\. ");
			text = text.trim().toLowerCase();
			String[] textArray = text.split("\\.\\s");
			return textArray;
		}
}
