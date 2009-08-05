/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ptolemy3d.app.tin.fortune;

public class DelTriangle{
	public int[] v;
	public int conn = 0;
	public int numpartners = 0;
	public boolean striped = false;
	public int id = 0;

	public DelTriangle[] partners = new DelTriangle[3];
	public int[] partner_ov = new int[3];
	
	private boolean on = true;

	public DelTriangle(int id , int v1 , int v2 , int v3 ){
		this.v = new int[3];
		this.id = id;
		v[0] = v1;
		v[1] = v2;
		v[2] = v3;
	}
	
	public void reset(){
		if(on){
			conn = numpartners;
			striped = false;
		}
	}
	
	public boolean edgeMatch(int v1 , int v2){
		if(  ((v[0] == v1) && (v[1] == v2)) ||
			((v[1] == v1) && (v[2] == v2)) ||
			((v[2] == v1) && (v[0] == v2))   )
				return true;
		else return false;		
	}
	
	public void setOff(){
		on = false;
		striped = true;
		decrementAllPartners();
	}

	public boolean match(int v1 , int v2 , int v3){
		int mct = 0;
		for(int i=0;i<3;i++){
			if(v1 == v[i]) mct++;
			else if(v2 == v[i]) mct++;
			else if(v3 == v[i]) mct++;
		}

		if(mct == 3) return true;
		else return false;
	}

	public void addPartner(DelTriangle dt, int[] simv){
		for(int i=0;i<conn;i++){
			if(dt == partners[i]) return;
		}

		if(conn >= partners.length){
			DelTriangle[] npart = new DelTriangle[partners.length + 3];
			System.arraycopy(partners , 0 , npart , 0 , partners.length);
			partners = npart;

			int[] n_ov = new int[partner_ov.length + 3];
			System.arraycopy(partner_ov , 0 , n_ov , 0 , partner_ov.length);
			partner_ov = n_ov;
		}

		for(int i=0;i<3;i++){
			if((v[i] != simv[0]) && (v[i] != simv[1])){
				partner_ov[conn] = v[i];
				break;
			}
		}
		partners[conn++] = dt;
		numpartners++;
	}

	public int setOddVert(DelTriangle dt){
		int ov = 0;
		if(conn > 0)conn--;

		for(int i=0;i<numpartners;i++){
			if(dt == partners[i]) ov = partner_ov[i];
			else{
				partners[i].conn--;
			}
		}

		return ov;
	}
	
	public int setOddVertLp(DelTriangle dt){
		int ov = 0;
		for(int i=0;i<numpartners;i++){
			if(dt == partners[i]) ov = partner_ov[i];
		}
		return ov;
	}
	
	public void decrementAllPartners(){
		for(int i=0;i<numpartners;i++){
			partners[i].conn--;
		}
	}
	
}
