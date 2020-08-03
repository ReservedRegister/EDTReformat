import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FormatFile implements Runnable
{
	private String file_path_edited;
	private String file_name;
	private List<String> file_lines;
	private int end_ent;
	private boolean no_new_line_global;
	
	public FormatFile(String file_name_in)
	{
		file_path_edited = "./edited/";
		file_name = file_name_in;
		emptyTheFile(file_path_edited, file_name);
		file_lines = readFile("./original/" + file_name);
		end_ent = 0;
		no_new_line_global = false;
	}
	
	private void emptyTheFile(String file_path, String file_name)
	{
		try(FileWriter writer = new FileWriter(file_path + file_name);
				BufferedWriter bw = new BufferedWriter(writer)) {} catch(IOException e) {}
	}
	
	private List<String> readFile(String file_name_in)
	{
		List<String> read_lines = new ArrayList<>();
		
		try(Scanner reader = new Scanner(new File(file_name_in)))
		{
			while(reader.hasNext())
			{
				read_lines.add(reader.nextLine());
			}
		}
		catch(FileNotFoundException e)
		{
			System.out.println("EDT File could not be opened!");
		}
		
		return read_lines;
	}
	
	private void createEDTFile()
	{
		try
		{
			new File(file_path_edited).mkdirs();
			new File(file_path_edited + file_name).createNewFile();
		}
		catch(IOException e) {}
	}
	
	public void writeNewEDTFile(String write_line, String end, boolean new_line)
	{
		createEDTFile();
		
		List<String> read_lines = readFile(file_path_edited + file_name);
		
		try(FileWriter writer = new FileWriter(file_path_edited + file_name); BufferedWriter bw = new BufferedWriter(writer))
		{
			for(int i = 0; i < read_lines.size(); i++)
			{
				String line = read_lines.get(i);
				bw.write(line);
				
				if(i == read_lines.size() - 1)
					if(no_new_line_global == true)
					{
						no_new_line_global = false;
						break;
					}
				
				bw.newLine();
			}
			
			bw.write(write_line);
			if(new_line) bw.newLine();
			
			if(!end.isEmpty())
			{
				bw.write(end);
				bw.newLine();
			}
		}
		catch(IOException e) {}
	}
	
	private int writeStart()
	{
		String write_line = "";
		write_line = file_name.replaceFirst(".edt", "") + "\n{\n";
		
		int console_section = hasConsoleSection();
		
		if(console_section != -1)
		{
			write_line = write_line + "	console\n	{";
			writeNewEDTFile(write_line, "", false);
			no_new_line_global = true;
			return copyConsoleCommands(console_section);
		}
		
		write_line = file_name.replaceFirst(".edt", "") + "\n{\n" + "	entity\n	{";
		writeNewEDTFile(write_line, "", true);
		return 0;
	}
	
	private int hasConsoleSection()
	{
		for(int i = 0; i < file_lines.size(); i++)
		{
			String file_line = file_lines.get(i).trim();
			
			if(file_line.replace("\"", "").startsWith("console"))
				return i;
		}
		
		return -1;
	}
	
	public int copyConsoleCommands(int start_index)
	{
		String items = "";
		int skip = 0;
		boolean found = false;
		
		for(int i = start_index; i < file_lines.size(); i++)
		{
			String file_line = file_lines.get(i);
			
			if(found)
			{
				if(file_line.contains("}"))
				{
					String item_to_add = file_line.substring(0, file_line.indexOf("}")).trim();
					
					if(!item_to_add.isEmpty())
						items = items + "\n" + "		" + item_to_add;
					
					skip = i;
					break;
				}
				
				items = items + "\n" + "		" + file_line.trim();
			}
			
			if(file_line.contains("{"))
			{
				String item_to_add = file_line.substring(file_line.indexOf("{") + 1).trim();
				
				if(!item_to_add.isEmpty())
					items = items + "\n" + "		" + item_to_add;
				
				found = true;
			}
		}
		
		writeNewEDTFile(items, "	}\n" + "	entity\n	{\n", true);
		return skip;
	}
	
	private void writeEnd()
	{
		String write_line = "	}\n}";
		writeNewEDTFile(write_line, "", true);
	}
	
	public void reformatEDT()
	{
		String comments_buffer = "";
		
		for(int i = writeStart(); i < file_lines.size(); i++)
		{
			String file_line = file_lines.get(i).replace("\"", "").trim();
			
			if(i > end_ent)
			{
				if(file_line.startsWith("//"))
					comments_buffer = comments_buffer + file_line + "\n";
				else
				{
					writeNewEDTFile(comments_buffer.trim(), "", true);
					comments_buffer = "";
				}
			}
			
			if(file_line.startsWith("create"))
			{
				reformatCREATE(i);
			}
			else if(file_line.startsWith("edit"))
			{
				reformatEDIT(i);
			}
			else if(file_line.startsWith("delete"))
			{
				reformatDELETE(i);
			}
		}
		
		writeEnd();
		
		System.out.println("Finished working on: " + file_name);
	}
	
	private List<Object> removeComments(String line, boolean in_quotations_last_line)
	{
		List<Object> data = new ArrayList<>();
		
		String removed_comments = line;
		boolean in_quotations = in_quotations_last_line;
		int found = 0;
		
		for(int i = 0; i < line.length(); i++)
		{
			if(line.charAt(i) == '"')
			{
				if(in_quotations == false)
					in_quotations = true;
				else
					in_quotations = false;
			}
			
			if(line.charAt(i) == '/')
			{
				found++;
			}
			else
				found = 0;
			
			if(found == 2 && !in_quotations)
			{
				removed_comments = line.substring(0, i-1);
				break;
			}
		}
		
		data.add(removed_comments);
		data.add(in_quotations);
		return data;
	}
	
	private String replaceAnyCase(String line, String replaceString)
	{
		List<Integer> skip_index = new ArrayList<>();
		int next = 0;
		
		for(int i = 0; i < line.length(); i++)
		{
			if(next < replaceString.length())
			{
				if(Character.toLowerCase(line.charAt(i)) == Character.toLowerCase(replaceString.charAt(next)))
				{
					skip_index.add(i);
					next++;
				}
				else
				{
					skip_index.clear();
					next = 0;
				}
			}
		}
		
		if(next >= replaceString.length())
		{
			String construct_string = "";
			
			for(int i = 0; i < line.length(); i++)
			{
				if(!skip_index.contains(i))
					construct_string = construct_string + line.charAt(i);
			}
			
			return construct_string;
		}
		
		return line;
	}
	
	private List<String> getFirstSection(int start_index)
	{
		String start_line = file_lines.get(start_index).trim();
		boolean in_quotations = false;
		List<Object> remove_comments_data = removeComments(start_line, in_quotations);
		
		start_line = (String) remove_comments_data.get(0);
		start_line = start_line.trim();
		in_quotations = (boolean) remove_comments_data.get(1);
		
		List<String> create_lines = new ArrayList<>();
		List<String> items = new ArrayList<>();
		List<Integer> start_brackets = searchItem(start_line, '{');
		
		int found_brackets = start_brackets.size();
		
		if(found_brackets == 1)
		{
			String start_line_copy = start_line.substring(start_brackets.get(0) + 1).replaceAll("}", "").trim();
			start_line_copy = replaceAnyCase(start_line_copy, "values").trim();
			
			if(!start_line_copy.isEmpty())
				create_lines.add(start_line_copy);
		}
		else if(found_brackets > 1)
		{
			String start_line_copy = start_line.substring(start_brackets.get(0) + 1, start_brackets.get(1)).replaceAll("}", "");
			start_line_copy = replaceAnyCase(start_line_copy, "values").trim();
			
			if(!start_line_copy.isEmpty())
				create_lines.add(start_line_copy);
		}
		
		if(!start_line.contains("}"))
		{
			for(int i = start_index+1; i < file_lines.size(); i++)
			{
				List<Object> data = removeComments(file_lines.get(i), in_quotations);
				String file_line = (String) data.get(0);
				file_line = file_line.trim();
				in_quotations = (boolean) data.get(1);
				
				file_line = replaceAnyCase(file_line, "values").trim();
				
				start_brackets = searchItem(file_line, '{');
				found_brackets = found_brackets + start_brackets.size();
				
				if(found_brackets > 1 || file_line.contains("}"))
				{
					if(!start_brackets.isEmpty())
						file_line = file_line.substring(0, start_brackets.get(0)).replaceAll("}", "").trim();
					if(!file_line.isEmpty() && !start_brackets.isEmpty())
						create_lines.add(file_line);
					
					end_ent = i;
					break;
				}
				
				if(!file_line.isEmpty() && !file_line.equals("{") && !file_line.startsWith("\"\"") && !file_line.startsWith("//"))
					create_lines.add(file_line.replaceAll("}", "").trim());
			}
		}
		else
			end_ent = start_index;
		
		for(String create_line : create_lines)
		{	
			if(create_line.trim().isEmpty())
				continue;
			
			in_quotations = false;
			int start = 0;
			
			for(int i = 0; i < create_line.length(); i++)
			{
				String actual_line = create_line.substring(start, i).replace("\"", "").trim();
				String add_line = "";
				
				if(create_line.charAt(i) == '"')
				{
					if(!in_quotations)
						in_quotations = true;
					else
						in_quotations = false;
					
					add_line = actual_line;
					start = i+1;
				}
				else if(create_line.charAt(i) == ' ' && !in_quotations)
				{
					add_line = actual_line;
					start = i+1;
				}
				else if(create_line.charAt(i) == '	' && !in_quotations)
				{
					add_line = actual_line;
					start = i+1;
				}
				
				if(i == create_line.length() - 1 && create_line.charAt(i) != '"')
					add_line = create_line.substring(start, i+1).replace("\"", "");
				
				if(!add_line.isEmpty() && !add_line.startsWith("//"))
					items.add(add_line);
			}
		}
		
		return items;
	}
	
	private List<Integer> searchItem(String item, char search)
	{
		List<Integer> places_occured = new ArrayList<>();
		
		for(int i = 0; i < item.length(); i++)
		{
			if(item.charAt(i) == search)
				places_occured.add(i);
		}
		
		return places_occured;
	}
	
	private List<String> getSecondSection(int start_index)
	{
		String start_line = file_lines.get(start_index).trim();
		boolean in_quotations = false;
		List<Object> remove_comments_data = removeComments(start_line, in_quotations);
		start_line = (String) remove_comments_data.get(0);
		start_line = start_line.trim();
		in_quotations = (boolean) remove_comments_data.get(1);
		
		List<String> value_lines = new ArrayList<>();
		List<String> items = new ArrayList<>();
		List<Integer> start_brackets = searchItem(start_line, '{');
		
		int found_brackets = start_brackets.size();
		boolean found = false;
		
		if(found_brackets > 1)
			value_lines.add(start_line.replace("}", "").substring(start_brackets.get(1) + 1).trim());
			
		if(!start_line.contains("}"))
		{
			for(int i = start_index+1; i < file_lines.size(); i++)
			{
				List<Object> data = removeComments(file_lines.get(i), in_quotations);
				String file_line = (String) data.get(0);
				file_line = file_line.trim();
				in_quotations = (boolean) data.get(1);
				
				if(file_line.isEmpty())
					value_lines.add("\n");
				
				file_line = file_line.trim();
				
				start_brackets = searchItem(file_line, '{');
				found_brackets = found_brackets + start_brackets.size();
				
				if(found_brackets > 1)
					found = true;
				
				if(!start_brackets.isEmpty())
				{
					file_line = file_line.substring(start_brackets.get(0) + 1).trim();
				}
				
				if(found && !file_line.isEmpty() && !file_line.startsWith("\"\"") && !file_line.startsWith("//"))
				{
					value_lines.add(file_line);
				}
				
				if(file_line.contains("}"))
				{
					end_ent = i;
					break;
				}
			}
		}
		else
			end_ent = start_index;
		
		in_quotations = false;
		String persistant_string = "";
		
		for(String value_line : value_lines)
		{
			if(value_line.trim().isEmpty() && !in_quotations)
				continue;
			
			int start = 0;
			
			for(int i = 0; i < value_line.length(); i++)
			{
				String actual_line = value_line.substring(start, i).replace("\"", "").trim();
				String add_line = "";
				
				if(value_line.charAt(i) == '"')
				{
					if(!in_quotations)
					{
						add_line = actual_line;
						in_quotations = true;
					}
					else
					{
						in_quotations = false;
						add_line = persistant_string + actual_line;
						
						if(actual_line.isEmpty() && persistant_string.isEmpty())
							add_line = "\"\"";
						
						persistant_string = "";
					}
					
					start = i+1;
				}
				else if(value_line.charAt(i) == ' ' && !in_quotations)
				{
					add_line = actual_line;
					start = i+1;
				}
				else if(value_line.charAt(i) == '	' && !in_quotations)
				{
					add_line = actual_line;
					start = i+1;
				}
				
				if(i == value_line.length() - 1 && value_line.charAt(i) != '"' && !in_quotations)
					add_line = value_line.substring(start, i+1).trim();
				
				if(i == value_line.length() - 1 && in_quotations)
				{
					String full_line = value_line.substring(start, i+1).trim();
					persistant_string = persistant_string + full_line + "\n";
				}
				
				if(!add_line.isEmpty() && !add_line.startsWith("//") && !add_line.contains("}"))
					items.add(add_line);
			}
		}
		
		return items;
	}
	
	// create - new lines
	// edit - same line (maybe more)
	// delete - same line (maybe more)
	
	// CREATE Format
	/* 
	 * create {values{}}
	 * */
	
	// CREATE PRIORITY
	// create
	// classname, origin
	// values
	// model,angles,spawnflags,StartDisabled,targetname
	
	private void reformatSectionDELETE(List<String> create_lines)
	{
		String write_line = "";
		
		for(int i = 0; i < create_lines.size(); i++)
		{
			String any_line = " " + create_lines.get(i) + " \"" + create_lines.get(i+1) + "\"";
			write_line = write_line + any_line;
			
			i = i+1;
		}
		
		write_line = write_line.trim();
		writeNewEDTFile("		delete {" + write_line, "}", false);
	}
	
	private void reformatFirstSectionEDIT(List<String> create_lines)
	{
		String write_line = "";
		
		for(int i = 0; i < create_lines.size(); i++)
		{
			String any_line = " " + create_lines.get(i) + " \"" + create_lines.get(i+1) + "\"";
			write_line = write_line + any_line;
			
			i = i+1;
		}
		
		write_line = write_line.trim();
		writeNewEDTFile("		edit {" + write_line, "", false);
		no_new_line_global = true;
	}
	
	private void reformatSecondSectionEDIT(List<String> value_lines)
	{
		String write_line = "";
		
		for(int i = 0; i < value_lines.size(); i++)
		{
			if(!value_lines.get(i+1).contains("\""))
			{
				String any_line = " " + value_lines.get(i) + " \"" + value_lines.get(i+1) + "\"";
				write_line = write_line + any_line;
			}
			else
			{
				String any_line = " " + value_lines.get(i) + " " + value_lines.get(i+1);
				write_line = write_line + any_line;
			}
			
			i = i+1;
		}
		
		write_line = write_line.trim();
		writeNewEDTFile(" values {" + write_line, "} }", false);
	}
	
	public String searchPriorityValue(List<String> value_lines, String seachItem)
	{
		for(int i = 0; i < value_lines.size(); i++)
		{
			if(value_lines.get(i).equalsIgnoreCase(seachItem))
			{
				String line = " " + value_lines.get(i) + " \"" + value_lines.get(i+1) + "\"";
				return line;
			}
			
			i = i+1;
		}
		
		return "";
	}
	
	private void reformatSecondSectionCREATE(List<String> value_lines)
	{
		String[] priority_values = {"model", "angles", "spawnflags", "StartDisabled", "targetname"};
		String write_line = "";
		boolean removed_value = false;
		
		for(String value : priority_values)
		{
			write_line = write_line + searchPriorityValue(value_lines, value);
			
			for(int i = 0; i < value_lines.size(); i++)
			{
				String value_line = value_lines.get(i);
				
				if(value_line.equalsIgnoreCase(value))
				{
					removed_value = true;
					value_lines.remove(i);
					value_lines.remove(i);
				}
			}
		}
		
		if(!value_lines.isEmpty())
			write_line = write_line + "\n";
		
		for(int i = 0; i < value_lines.size(); i++)
		{
			if(i == value_lines.size() - 2)
			{
				String line = value_lines.get(i) + " \"" + value_lines.get(i+1) + "\"";
				write_line = write_line + "				" + line;
				break;
			}
			
			String line = value_lines.get(i) + " \"" + value_lines.get(i+1) + "\"\n";
			write_line = write_line + "				" + line;
			
			i = i+1;
		}
		
		if(removed_value)
			write_line = write_line.trim();
		
		writeNewEDTFile("			values {" + write_line, "			} }", true);
	}
	
	private void reformatFirstSectionCREATE(List<String> create_lines)
	{
		String[] priority_values = {"classname", "origin"};
		String write_line = "";
		
		for(String value : priority_values)
		{
			write_line = write_line + searchPriorityValue(create_lines, value);
			
			for(int i = 0; i < create_lines.size(); i++)
			{
				String value_line = create_lines.get(i);
				
				if(value_line.equalsIgnoreCase(value))
				{
					create_lines.remove(i);
					create_lines.remove(i);
				}
			}
		}
		
		for(int i = 0; i < create_lines.size(); i++)
		{
			String line = create_lines.get(i) + " \"" + create_lines.get(i+1) + "\"";
			write_line = write_line + " " + line;
			
			i = i+1;
		}
		
		write_line = write_line.trim();
		writeNewEDTFile("		create {" + write_line, "", true);
	}
	
	private void reformatCREATE(int start_index)
	{
		List<String> first_section = getFirstSection(start_index);
		List<String> second_section = getSecondSection(start_index);
		
		reformatFirstSectionCREATE(first_section);
		reformatSecondSectionCREATE(second_section);
	}
	
	private void reformatEDIT(int start_index)
	{
		List<String> first_section = getFirstSection(start_index);
		List<String> second_section = getSecondSection(start_index);
		
		reformatFirstSectionEDIT(first_section);
		reformatSecondSectionEDIT(second_section);
	}
	
	private void reformatDELETE(int start_index)
	{
		List<String> first_section = getFirstSection(start_index);
		
		reformatSectionDELETE(first_section);
	}

	@Override
	public void run()
	{
		reformatEDT();
	}
}
