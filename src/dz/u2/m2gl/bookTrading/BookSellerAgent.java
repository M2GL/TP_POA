
package dz.u2.m2gl.bookTrading;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
	// Le catalogue des livres à vendre (associe le titre d'un livre à son prix)
	private Hashtable catalogue;
	// L'interface graphique au moyen de laquelle l'utilisateur peut ajouter des livres dans le catalogue
	private BookSellerGui myGui;
	Random rand = new Random();
	private int sold = rand.nextInt(100) + 1;;
	String title;
	Integer price;

	
	//Placez les initialisations d'agent ici
	protected void setup() {
		// Créer le catalogue
		catalogue = new Hashtable();

		//Créer et afficher l'interface graphique
		myGui = new BookSellerGui(this);
		myGui.showGui();

		// Enregistrez le service de vente de livres dans les pages jaunes
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		//Ajouter le comportement servant les requêtes des agents acheteurs
		addBehaviour(new OfferRequestsServer());

		// Ajouter le comportement servant les bons de commande des agents acheteurs
		addBehaviour(new PurchaseOrdersServer());
	}

	// Placez les opérations de nettoyage des agents ici
	protected void takeDown() {
		// Se désinscrire des pages jaunes
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Fermez l'interface graphique
		myGui.dispose();
		// Imprimer un message de licenciement
		System.out.println("Vendeur-agent"+getAID().getName().split("@")[0]+" terminer.");
	}

	/**
     Ceci est invoqué par l'interface graphique lorsque l'utilisateur ajoute un nouveau livre à vendre
	 */
	public void updateCatalogue(final String title, final int price) {
		
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(title, new Integer(price));
				System.out.println(getAID().getName().split("@")[0]+"   "+title+" inséré dans le catalogue. Prix = "+price);
			}
		} );
		
     
	}

	
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			//System.out.println("getContent::00-"+msg);
			if (msg != null) {
				//System.out.println("getContent::"+msg.getContent());
				if(msg.getContent().equals("sold")){
					int p = (price.intValue()- sold);
					ACLMessage soldreply = msg.createReply();
					System.out.println(getAID().getName().split("@")[0]+": oui, nous avons une première promo client sur le livre: "+title+" le prix final sera "+p);
					soldreply.setPerformative(ACLMessage.PROPOSE);
					soldreply.setContent(String.valueOf(p));	
					//System.out.println("sent:"+soldreply);
					myAgent.send(soldreply);
					//System.out.println("donnnne");
					
				}
				// CFP Message reçu. Traitement
				ACLMessage reply = msg.createReply();
				title = msg.getContent();
				price = (Integer) catalogue.get(title);
				
				if (price != null) {
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
					System.out.println(getAID().getName().split("@")[0]+": bonjour, le prix du livre "+title+" est= "+price);
}
				
				else {
					
					// Le livre demandé n'est PAS disponible à la vente.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // Fin de la classe interne OfferRequestsServer

	/**
	   Classe interne PurchaseOrdersServer.
Il s'agit du comportement utilisé par les agents Book-seller pour servir les appels entrants
offrir des acceptations (c'est-à-dire des bons de commande) des agents acheteurs.
L'agent vendeur supprime le livre acheté de son catalogue
et répond par un message INFORM pour informer l'acheteur que le
l'achat a été complété avec succès.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message reçu. Traitement
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer price = (Integer) catalogue.remove(title);
				if (price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(title+" vendu à l'agent "+msg.getSender().getName());
				}
				else {
					// Le livre demandé a été vendu à un autre acheteur entre-temps.
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // Fin de la classe interne OfferRequestsServer
}
