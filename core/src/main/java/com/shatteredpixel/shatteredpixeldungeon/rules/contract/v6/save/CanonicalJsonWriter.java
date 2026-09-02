package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tiny deterministic JSON writer: caller controls canonical field order. */
final class CanonicalJsonWriter {
	private final StringBuilder out=new StringBuilder();private final Deque<Boolean> first=new ArrayDeque<>();private boolean afterName;
	CanonicalJsonWriter beginObject(){beforeValue();out.append('{');first.push(Boolean.TRUE);return this;}
	CanonicalJsonWriter endObject(){out.append('}');first.pop();return this;}
	CanonicalJsonWriter beginArray(){beforeValue();out.append('[');first.push(Boolean.TRUE);return this;}
	CanonicalJsonWriter endArray(){out.append(']');first.pop();return this;}
	CanonicalJsonWriter name(String name){element();quote(name);out.append(':');afterName=true;return this;}
	CanonicalJsonWriter value(String value){beforeValue();if(value==null)out.append("null");else quote(value);return this;}
	CanonicalJsonWriter value(long value){beforeValue();out.append(value);return this;}
	CanonicalJsonWriter value(boolean value){beforeValue();out.append(value);return this;}
	private void beforeValue(){if(afterName){afterName=false;return;}if(!first.isEmpty())element();}
	private void element(){if(first.isEmpty())return;if(first.pop())first.push(Boolean.FALSE);else{first.push(Boolean.FALSE);out.append(',');}}
	private void quote(String value){out.append('"');for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case'"':out.append("\\\"");break;case'\\':out.append("\\\\");break;case'\b':out.append("\\b");break;case'\f':out.append("\\f");break;case'\n':out.append("\\n");break;case'\r':out.append("\\r");break;case'\t':out.append("\\t");break;default:if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}out.append('"');}
	@Override public String toString(){if(!first.isEmpty()||afterName)throw new IllegalStateException("unfinished JSON");return out.toString();}
}
