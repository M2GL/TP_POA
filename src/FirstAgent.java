import jade.core.Agent;

public class FirstAgent extends Agent {
   
	@Override
	protected void setup() {
	    System.out.println("Bonjour tous le monde, Je suis :  " + this.getLocalName());
	}
}
