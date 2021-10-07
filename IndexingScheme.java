import java.util.LinkedList;
import graph.*;

import java.io.File;
import java.io.FileWriter;


class IndexingScheme{
	int indexes[][];
	int vertex_c[];
	int deletedEdges=0;
	int examinedEdges=0;
	//int vertex_c_index[];
	public IndexingScheme(SimpleGraph G,LinkedList<Channel> decomposition,IVertex[] topological_order){
		LinkedList<IVertex> stacks[];
		indexes = new int[G.getVertices().size()][decomposition.size()];
		vertex_c = new int[G.getVertices().size()];
		stacks = new LinkedList[G.getVertices().size()];
		
		for(int i=0;i<G.getVertices().size();++i){
			stacks[i] = new LinkedList<IVertex>();
			for(int j=0;j<decomposition.size();++j){
				indexes[i][j]=Integer.MAX_VALUE;
			}
		}
		for(int j=0;j<decomposition.size();++j){
			Channel c = decomposition.get(j);
			for(int i=0;i< c.getVertices().size()-1;++i ){
				IVertex v = c.getVertices().get(i);
				int id=(int)v.getId();
				//System.out.println("id:"+id+" channel:"+j+" label:"+v.getLabel());
				vertex_c[id]=j;
				//indexes[id][j] = i+1;
				//vertex_c_index=i;
			}
			int last = c.getVertices().size()-1;
			IVertex v = c.getVertices().get(last);
			int id=(int)v.getId();
			vertex_c[id]=j;
			//System.out.println("id:"+id+" channel:"+j+" label:"+v.getLabel());
		}
		//printIndexingScheme();
		System.out.println("initialization completed");

		for(int i=topological_order.length-1;i>=0;--i){
			IVertex cur = topological_order[i];
			int cur_id = (int)cur.getId();
			for( IVertex v:cur.getAdjacentSources() ){
				int s_id=(int)v.getId();
				stacks[s_id].addLast(cur);
			}
			
			while(stacks[cur_id].isEmpty()==false){
				IVertex tmp = stacks[cur_id].removeLast();
				int tmp_id = (int)tmp.getId();
				
				int tmp_channel = vertex_c[tmp_id];
				int next = indexes[tmp_id][tmp_channel];
				int tmp_pos;
				if(next!=Integer.MAX_VALUE){
					tmp_pos=next-1;
				}else{
					tmp_pos=decomposition.get(tmp_channel).getVertices().size()-1;
				}
				//System.out.println("-->cur:"+cur_id+" tmp:"+tmp_id+" tmp_pos:"+tmp_pos);
				if(indexes[cur_id][vertex_c[ tmp_id] ]<= tmp_pos){
					//do nothing
					deletedEdges++;
				}else{
					examinedEdges++;
					indexes[cur_id][tmp_channel]=tmp_pos;
					merge_indexes(indexes[cur_id] , indexes[tmp_id]);
				}
			}
			stacks[cur_id]=null;
		}
	}
	public void printEdges(){
		System.out.println("-------->del:"+deletedEdges+" ex:"+examinedEdges);
	}
	private void merge_indexes(int cur[] , int tmp[]){
		//System.out.println("  merge:");
		for(int i=0;i<cur.length;++i){
			if(cur[i]>tmp[i]){
				cur[i]=tmp[i];
			}
		}
	}
	
	/*private int getChannelSucc(int vertex_id){
		int channel = vertex_c[vertex_id];
		int next = indexes[vertex_id][channel];
		return next;
	}*/
	
	public void printIndexingScheme(){
		int counter=0;
		for(int vi[]:indexes){
			System.out.print(""+counter+":");
			for(int i:vi){
				System.out.print(" "+i);
			}
			System.out.println("");
			counter++;
		}
	}
	
	public static void main(String[]args) {
		//LinkedList<SimpleGraph>graphs2 = new LinkedList<SimpleGraph>();
		Reader r = new Reader();
		final File folder = new File("F:\\courses\\master_thesis\\Graph decomposition code\\code_for_the_student\\inputgraphs");
		//LinkedList<String> filenames = new LinkedList<String>();
		LinkedList<File> files = new LinkedList<File>();
		//listFilesForFolder(folder,filenames);
		Main.listFilesForFolder(folder,files);

		Heuristics h = new Heuristics();
		try{
			int graphs_num = 0;
			double aggregate = 0;
			double aggregate_opt = 0;
			
			double del_e = 0;
			double exam_e = 0;
			double total_edges = 0;
			
			int counter = 0;
			long sum=0;
			for(File f: files) {
				System.out.println(""+(++counter)+":"+f.getName());
				graphs_num +=1;
				SimpleGraph G = r.read(f);
				G.setAdjacency();
				Main.setTopologicalIds(G);
				IVertex []array = new IVertex[ G.getVertices().size() ];
				int[] adj_no = new int[ G.getVertices().size() ];
				for( IVertex v:G.getVertices() ){
					array[(int)v.getId()] = v;
					adj_no[(int)v.getId()] = v.getAdjacentSources().size()-1;
				}
				System.out.println("start processing");
				long startTime = System.currentTimeMillis(); 
				LinkedList<Channel> decomposition =  h.newMethod1(G,adj_no);//h.MyHeuristic(G,-1);//h.DAG_decomposition_Fulkerson(G);h.newMethod1_fastest(G,adj_no);//
				long stopTime = System.currentTimeMillis();
				sum=(stopTime-startTime);
				//Main.printDecomposition(decomposition);
				System.out.println("finish processing");
				LinkedList<Channel> decomposition_f=h.DAG_decomposition_Fulkerson(G);
				System.out.println("dec size:"+decomposition.size()+ " time:"+sum);
				System.out.println("opt width:"+decomposition_f.size());
				if(Heuristics.checkDecomposition(G,decomposition)==false){
					System.out.println(f);
					System.exit(0);
				}
				aggregate_opt+=decomposition_f.size();
				aggregate+=decomposition.size();
				System.out.println("start building indexing scheme");
				startTime = System.currentTimeMillis(); 
				IndexingScheme scheme=new IndexingScheme( G,decomposition,array);
				stopTime = System.currentTimeMillis();
				//scheme.printIndexingScheme();
				System.out.println("indexing scheme accomplished, time:"+(stopTime-startTime));
				scheme.printEdges();
				del_e+=scheme.deletedEdges;
				exam_e+=scheme.examinedEdges;
				total_edges+=(scheme.deletedEdges+scheme.examinedEdges);
			}
			System.out.println("time:"+sum);
			System.out.println("av_opt_dec:"+(aggregate_opt/graphs_num)+" av_dec_heur:"+(aggregate/graphs_num));
			System.out.println("average deleted edges:"+(del_e/total_edges));
		}catch (Exception e) {  
            e.printStackTrace();  
        }
	}		
}