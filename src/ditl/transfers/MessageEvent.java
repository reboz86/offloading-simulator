/*******************************************************************************
 * This file is part of DITL.                                                  *
 *                                                                             *
 * Copyright (C) 2011-2012 John Whitbeck <john@whitbeck.fr>                    *
 *                                                                             *
 * DITL is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU General Public License as published by        *
 * the Free Software Foundation, either version 3 of the License, or           *
 * (at your option) any later version.                                         *
 *                                                                             *
 * DITL is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               *
 * GNU General Public License for more details.                                *
 *                                                                             *
 * You should have received a copy of the GNU General Public License           *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.       *
 *******************************************************************************/
package ditl.transfers;

import ditl.ItemFactory;

public class MessageEvent {
	public final static boolean NEW = true;
	public final static boolean EXPIRE = false;
	
	Integer msg_id;
	boolean is_new;
	Long _size;
	
	public MessageEvent(Integer msgId, boolean isNew){
		msg_id = msgId;
		is_new = isNew;
	}
	
	public MessageEvent(Integer msgId, boolean isNew, Long size){
		msg_id = msgId;
		is_new = isNew;
		_size=size;
	}
	
	public Integer msgId(){
		return msg_id;
	}
	
	public boolean isNew(){
		return is_new;
	}
	
	public Long size(){
		return _size;
	}
	
	public Message message(){
		return new Message(msg_id);
	}
	
	public static final class Factory implements ItemFactory<MessageEvent> {
		@Override
		public MessageEvent fromString(String s) {
			String[] elems = s.trim().split(" ");
			try {
				Integer msgId = Integer.parseInt(elems[0]);
				boolean isNew = elems[1].equals("NEW");
				
				if ( isNew){
					long size = Integer.parseInt(elems[2]);
					return new MessageEvent(msgId, isNew,size);
				}
				else
					return new MessageEvent(msgId, isNew);
			} catch ( Exception e ){
				System.err.println( "Error parsing '"+s+"': "+e.getMessage() );
				return null;
			}
		}
	}
	
	@Override
	public String toString(){
		if ( is_new )
			return msg_id+" NEW "+ _size;
		return msg_id+" EXPIRE";
	}
}
