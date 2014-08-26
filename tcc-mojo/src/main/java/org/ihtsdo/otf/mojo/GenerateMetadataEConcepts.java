/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.mojo;

import java.beans.PropertyVetoException;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDynamicCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic;
import org.ihtsdo.otf.tcc.api.metadata.binding.Search;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicColumnInfo;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataType;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicUsageDescription;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpecWithDescriptions;
import org.ihtsdo.otf.tcc.api.spec.RelSpec;
import org.ihtsdo.otf.tcc.api.uuid.UuidT5Generator;
import org.ihtsdo.otf.tcc.dto.TtkConceptChronicle;
import org.ihtsdo.otf.tcc.dto.component.TtkComponentChronicle;
import org.ihtsdo.otf.tcc.dto.component.TtkRevision;
import org.ihtsdo.otf.tcc.dto.component.attribute.TtkConceptAttributesChronicle;
import org.ihtsdo.otf.tcc.dto.component.description.TtkDescriptionChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.TtkRefexAbstractMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid.TtkRefexUuidMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.TtkRefexDynamicMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.TtkRefexDynamicData;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicBoolean;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicByteArray;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicDouble;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicFloat;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicInteger;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicLong;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicNid;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicString;
import org.ihtsdo.otf.tcc.dto.component.refexDynamic.data.dataTypes.TtkRefexDynamicUUID;
import org.ihtsdo.otf.tcc.dto.component.relationship.TtkRelationshipChronicle;

/**
 * {@link GenerateMetadataEConcepts}
 * 
 * A utility class to pick up {@link ConceptSpec} entries, and write them out to an eConcept file.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 *
 * @goal generate-metadata-eConcepts
 * 
 * @phase process-sources
 */
public class GenerateMetadataEConcepts extends AbstractMojo
{
	private final UUID authorUuid_ = TermAux.USER.getUuids()[0];
	private final UUID pathUUID_ = TermAux.WB_AUX_PATH.getUuids()[0];
	private final UUID moduleUuid_ = TtkRevision.unspecifiedModuleUuid;
	private final long defaultTime_ = System.currentTimeMillis();
	private final LanguageCode lang_ = LanguageCode.EN;
	private final UUID isARelUuid_ = Snomed.IS_A.getUuids()[0];
	private final UUID definingCharacteristicUuid_ = SnomedMetadataRf2.STATED_RELATIONSHIP_RF2.getUuids()[0];
	private final UUID notRefinableUuid = SnomedMetadataRf2.NOT_REFINABLE_RF2.getUuids()[0];
	private final UUID descriptionAcceptableUuid_ = SnomedMetadataRf2.ACCEPTABLE_RF2.getUuids()[0];
	private final UUID descriptionPreferredUuid_ = SnomedMetadataRf2.PREFERRED_RF2.getUuids()[0];
	private final UUID usEnRefsetUuid_ = SnomedMetadataRf2.US_ENGLISH_REFSET_RF2.getUuids()[0];
	private final UUID refsetMemberTypeNormalMemberUuid_ = UUID.fromString("cc624429-b17d-4ac5-a69e-0b32448aaf3c"); //normal member

	private static enum DescriptionType
	{
		FSN, SYNONYM, DEFINITION;

		public UUID getTypeUUID()
		{
			if (this == FSN)
			{
				return Snomed.FULLY_SPECIFIED_DESCRIPTION_TYPE.getUuids()[0];
			}
			else if (this == SYNONYM)
			{
				return Snomed.SYNONYM_DESCRIPTION_TYPE.getUuids()[0];
			}
			else if (this == DEFINITION)
			{
				return Snomed.DEFINITION_DESCRIPTION_TYPE.getUuids()[0];
			}
			throw new RuntimeException("impossible");
		}
	}

	/**
	 * Name and location of the output file.
	 * 
	 * @parameter expression="${project.build.directory}/metadataEConcepts.jbin"
	 * @required
	 */
	private File outputFile;

	/**
	 * Fully specified class names which should be scanned for public, static variables which are instances of {@link ConceptSpec}.
	 * 
	 * Each {@link ConceptSpec} found will be output to the eConcept file.
	 * 
	 * @parameter
	 * @optional
	 */
	private String[] classesWithConceptSpecs;

	/**
	 * Any other {@link ConceptSpec} which should be built into the eConcept file.
	 *
	 * @parameter
	 * @optional
	 */
	private ConceptSpec[] conceptSpecs;

	/**
	 * Instead of writing the default jbin format, write the eccs change set format instead.
	 *
	 * @optional
	 */
	private boolean writeAsChangeSetFormat = false;
	
	/**
	 * This constructor is just for Maven - don't use.
	 */
	public GenerateMetadataEConcepts()
	{
		
	}
	
	/**
	 * Constructor for programmatic (non maven) access
	 * @param outputFile
	 * @param classesWithConceptSpecs
	 * @param conceptSpecs
	 * @param writeAsChangeSetFormat
	 */
	public GenerateMetadataEConcepts(File outputFile, String[] classesWithConceptSpecs, ConceptSpec[] conceptSpecs, boolean writeAsChangeSetFormat)
	{
		this.outputFile = outputFile;
		this.classesWithConceptSpecs = classesWithConceptSpecs;
		this.conceptSpecs = conceptSpecs;
		this.writeAsChangeSetFormat = writeAsChangeSetFormat;
	}

	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			outputFile = outputFile.getAbsoluteFile();
			outputFile.getParentFile().mkdirs();
			if (!outputFile.getParentFile().exists())
			{
				throw new MojoExecutionException("Cannot create the folder " + outputFile.getParentFile().getAbsolutePath());
			}

			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
			
			ArrayList<ConceptSpec> conceptSpecsToProcess = new ArrayList<>();
			

			if (conceptSpecs != null)
			{
				for (ConceptSpec cs : conceptSpecs)
				{
					conceptSpecsToProcess.add(cs);
				}
			}
			if (classesWithConceptSpecs != null)
			{
				for (String cs : classesWithConceptSpecs)
				{
					conceptSpecsToProcess.addAll(getSpecsFromClass(cs));
				}
			}
			
			int count = 0;
			for (ConceptSpec cs : conceptSpecsToProcess)
			{
				TtkConceptChronicle converted = convert(cs);
				if (RefexDynamic.REFEX_DYNAMIC_DEFINITION.getUuids()[0].equals(cs.getUuids()[0]))
				{
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					columns.add(new RefexDynamicColumnInfo(0, RefexDynamic.REFEX_COLUMN_ORDER.getUuids()[0], RefexDynamicDataType.INTEGER, null, true, null, null));
					columns.add(new RefexDynamicColumnInfo(1, RefexDynamic.REFEX_COLUMN_NAME.getUuids()[0], RefexDynamicDataType.UUID, null, true, null, null));
					columns.add(new RefexDynamicColumnInfo(2, RefexDynamic.REFEX_COLUMN_TYPE.getUuids()[0], RefexDynamicDataType.STRING, null, true, null, null));
					columns.add(new RefexDynamicColumnInfo(3, RefexDynamic.REFEX_COLUMN_DEFAULT_VALUE.getUuids()[0], RefexDynamicDataType.POLYMORPHIC, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(4, RefexDynamic.REFEX_COLUMN_REQUIRED.getUuids()[0], RefexDynamicDataType.BOOLEAN, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(5, RefexDynamic.REFEX_COLUMN_VALIDATOR.getUuids()[0], RefexDynamicDataType.STRING, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(6, RefexDynamic.REFEX_COLUMN_VALIDATOR_DATA.getUuids()[0], RefexDynamicDataType.POLYMORPHIC, null, false, null, null));
					
					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"This concept is used as an assemblage for defining new Refex extensions.  "
							+ "The attached data columns describe what columns are required to define a new Refex. ", columns);
				}
				else if (RefexDynamic.REFEX_DYNAMIC_DEFINITION_DESCRIPTION.getUuids()[0].equals(cs.getUuids()[0]))
				{
					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"This is the extended description type that must be attached to a description within a concept to make the concept valid for use as an "
									+ "assemblage concept for RefexDynamic refexes.  The description annotated with this type describes the intent of "
									+ "using the concept containing the description as an assemblage concept.", new RefexDynamicColumnInfo[0]);
				}
				else if (Search.SEARCH_GLOBAL_ATTRIBUTES.getUuids()[0].equals(cs.getUuids()[0])) {
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					//TODO [JOEL] Are these required, or optional?  I made everything optional, for now...
					columns.add(new RefexDynamicColumnInfo(0, Search.VIEW_COORDINATE_COLUMN.getUuids()[0], RefexDynamicDataType.BYTEARRAY, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(1, Search.MAX_RESULTS_COLUMN.getUuids()[0], RefexDynamicDataType.INTEGER, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(2, Search.DROOLS_EXPR_COLUMN.getUuids()[0], RefexDynamicDataType.STRING, null, false, null, null));

					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"Search Global Attributes is for attributes effecting all filters on a search concept", columns);
				}
				else if (Search.SEARCH_FILTER_ATTRIBUTES.getUuids()[0].equals(cs.getUuids()[0])) {
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					columns.add(new RefexDynamicColumnInfo(0, Search.ORDER_COLUMN.getUuids()[0], RefexDynamicDataType.INTEGER, null, false, null, null));
					columns.add(new RefexDynamicColumnInfo(1, Search.FILTER_INVERT_COLUMN.getUuids()[0], RefexDynamicDataType.BOOLEAN, null, false, null, null));

					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"Search Type Attributes is for attributes effecting all filters of a certain type such as Lucene or RegExp", columns);
				}
				else if (Search.SEARCH_LUCENE_FILTER.getUuids()[0].equals(cs.getUuids()[0])) {
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					columns.add(new RefexDynamicColumnInfo(0, Search.PARAMETER_COLUMN.getUuids()[0], RefexDynamicDataType.STRING, null, false, null, null));

					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"Search Lucene Filter is for attributes effecting this Lucene search", columns);
				}
				else if (Search.SEARCH_REGEXP_FILTER.getUuids()[0].equals(cs.getUuids()[0])) {
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					columns.add(new RefexDynamicColumnInfo(0, Search.PARAMETER_COLUMN.getUuids()[0], RefexDynamicDataType.STRING, null, false, null, null));

					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"Search RegExp Filter is for attributes effecting this RegExp search", columns);
				}
				else if (Search.SEARCH_ISDESCENDANTOF_FILTER.getUuids()[0].equals(cs.getUuids()[0])) {
					List<RefexDynamicColumnInfo> columns = new ArrayList<>();
					columns.add(new RefexDynamicColumnInfo(0, Search.ASCENDANT_COLUMN.getUuids()[0], RefexDynamicDataType.UUID, null, false, null, null));

					turnConceptIntoDynamicRefexAssemblageConcept(converted, true, 
							"Search IsDescendantOf Filter is for attributes effecting this IsDescendantOf search", columns);
				}
				
				if (writeAsChangeSetFormat)
				{
					dos.writeLong(System.currentTimeMillis());
				}
				converted.writeExternal(dos);
				count++;
			}
			dos.flush();
			dos.close();
			getLog().info("Wrote " + count + " concepts to " + outputFile.getAbsolutePath() + ".");
		}
		catch (IOException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException | NoSuchAlgorithmException | PropertyVetoException e)
		{
			throw new MojoExecutionException("Failure", e);
		}
	}
	
	private TtkRefexDynamicMemberChronicle addDynamicAnnotation(TtkComponentChronicle<?> component, UUID assemblageID, TtkRefexDynamicData[] data) 
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		TtkRefexDynamicMemberChronicle annotation = new TtkRefexDynamicMemberChronicle();
		annotation.setComponentUuid(component.getPrimordialComponentUuid());
		annotation.setRefexAssemblageUuid(assemblageID);
		annotation.setData(data);
		StringBuilder sb = new StringBuilder();
		sb.append(annotation.getRefexAssemblageUuid().toString()); 
		sb.append(annotation.getComponentUuid().toString());
		if (data != null)
		{
			for (TtkRefexDynamicData d : data)
			{
				if (d == null)
				{
					sb.append("null");
				}
				else
				{
					sb.append(d.getRefexDataType());
					sb.append(d.getData());
				}
			}
		}
		annotation.setPrimordialComponentUuid(UuidT5Generator.get(RefexDynamicCAB.refexDynamicNamespace, sb.toString()));
		setRevisionAttributes(annotation, null, null);
		if (component.getAnnotationsDynamic() == null)
		{
			component.setAnnotationsDynamic(new ArrayList<TtkRefexDynamicMemberChronicle>());
		}
		component.getAnnotationsDynamic().add(annotation);
		return annotation;
	}
	
	private List<ConceptSpec> getSpecsFromClass(String className) throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException
	{
		ArrayList<ConceptSpec> results = new ArrayList<>();
		
		Class<?> clazz = getClass().getClassLoader().loadClass(className);
		
		for (Field f : clazz.getFields())
		{
			if (f.getType().equals(ConceptSpec.class) || f.getType().equals(ConceptSpecWithDescriptions.class))
			{
				results.add((ConceptSpec)f.get(null));
			}
		}
		getLog().info("Got " + results.size() + " concept specs from " + className);
		return results;
	}

	private TtkConceptChronicle convert(ConceptSpec cs) throws IOException
	{
		TtkConceptChronicle cc = new TtkConceptChronicle();

		if (cs.getUuids().length < 1)
		{
			throw new IOException("Concept Spec " + cs + " does not have a UUID");
		}

		cc.setPrimordialUuid(cs.getUuids()[0]);

		TtkConceptAttributesChronicle conceptAttributes = new TtkConceptAttributesChronicle();
		conceptAttributes.setDefined(false);
		conceptAttributes.setPrimordialComponentUuid(cs.getUuids()[0]);
		setRevisionAttributes(conceptAttributes, null, null);
		cc.setConceptAttributes(conceptAttributes);

		for (int i = 1; i < cs.getUuids().length; i++)
		{
			cc.getConceptAttributes().getUuids().add(cs.getUuids()[i]);
		}
		
		if (cs instanceof ConceptSpecWithDescriptions)
		{
			ConceptSpecWithDescriptions csd = (ConceptSpecWithDescriptions)cs;
			addDescription(cc, csd.getFsn(), DescriptionType.FSN, true);
			
			boolean first = true;
			
			for (String s : csd.getSynonyms())
			{
				addDescription(cc, s, DescriptionType.SYNONYM, first);
				first = false;
			}
			first = true;
			for (String s : csd.getDefinitions())
			{
				addDescription(cc, s, DescriptionType.DEFINITION, first);
			}
			
		}
		else
		{
			addDescription(cc, cs.getDescription(), DescriptionType.FSN, true);
			addDescription(cc, cs.getDescription(), DescriptionType.SYNONYM, true);
		}

		for (RelSpec rs : cs.getRelSpecs())
		{
			addRelationship(cc, rs.getDestinationSpec().getUuids()[0], rs.getRelTypeSpec().getUuids()[0], null);
		}
		return cc;
	}

	/**
	 * Set up all the boilerplate stuff.
	 * 
	 * @param object - The object to do the setting to
	 * @param statusUuid - Uuid or null (for current)
	 * @param time - time or null (for global default value - essentially 'now')
	 */
	private void setRevisionAttributes(TtkRevision object, Status status, Long time)
	{
		object.setAuthorUuid(authorUuid_);
		object.setModuleUuid(moduleUuid_);
		object.setPathUuid(pathUUID_);
		object.setStatus(status == null ? Status.ACTIVE : status);
		object.setTime(time == null ? defaultTime_ : time.longValue());
	}

	/**
	 * Add a description to the concept.
	 * 
	 * @param time - if null, set to the time on the concept.
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	private TtkDescriptionChronicle addDescription(TtkConceptChronicle eConcept, String descriptionValue, DescriptionType wbDescriptionType, boolean preferred)
	{
		try
		{
			List<TtkDescriptionChronicle> descriptions = eConcept.getDescriptions();
			if (descriptions == null)
			{
				descriptions = new ArrayList<TtkDescriptionChronicle>();
				eConcept.setDescriptions(descriptions);
			}
			TtkDescriptionChronicle description = new TtkDescriptionChronicle();
			description.setConceptUuid(eConcept.getPrimordialUuid());
			description.setLang(lang_.getFormatedLanguageNoDialectCode());
			//This aligns with what DescriptionCAB does
			description.setPrimordialComponentUuid(UuidT5Generator.get(DescriptionCAB.descSpecNamespace,
					eConcept.getPrimordialUuid().toString() + wbDescriptionType.getTypeUUID() + lang_.getFormatedLanguageNoDialectCode() + descriptionValue));

			description.setTypeUuid(wbDescriptionType.getTypeUUID());
			description.setText(descriptionValue);
			setRevisionAttributes(description, Status.ACTIVE, eConcept.getConceptAttributes().getTime());

			descriptions.add(description);
			//Add the en-us info
			addUuidAnnotation(description, (preferred ? descriptionPreferredUuid_ : descriptionAcceptableUuid_), usEnRefsetUuid_);

			return description;
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			throw new RuntimeException("Shouldn't be possible");
		}
	}

	/**
	 * @param time - If time is null, uses the component time.
	 * @param valueConcept - if value is null, it uses RefsetAuxiliary.Concept.NORMAL_MEMBER.getPrimoridalUid()
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	private TtkRefexUuidMemberChronicle addUuidAnnotation(TtkComponentChronicle<?> component, UUID valueConcept, UUID refsetUuid)
	{
		try
		{
			List<TtkRefexAbstractMemberChronicle<?>> annotations = component.getAnnotations();

			if (annotations == null)
			{
				annotations = new ArrayList<TtkRefexAbstractMemberChronicle<?>>();
				component.setAnnotations(annotations);
			}

			TtkRefexUuidMemberChronicle conceptRefexMember = new TtkRefexUuidMemberChronicle();

			conceptRefexMember.setComponentUuid(component.getPrimordialComponentUuid());
			//This aligns with what RefexCAB does
			conceptRefexMember.setPrimordialComponentUuid(UuidT5Generator.get(RefexCAB.refexSpecNamespace, RefexType.MEMBER.name() + refsetUuid.toString()
					+ component.getPrimordialComponentUuid().toString()));
			conceptRefexMember.setUuid1(valueConcept == null ? refsetMemberTypeNormalMemberUuid_ : valueConcept);
			conceptRefexMember.setRefexExtensionUuid(refsetUuid);
			setRevisionAttributes(conceptRefexMember, Status.ACTIVE, component.getTime());

			annotations.add(conceptRefexMember);

			return conceptRefexMember;
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			throw new RuntimeException("Shouldn't be possible");
		}
	}
	
	/**
	 * Add a relationship. The source of the relationship is assumed to be the specified concept.
	 * 
	 * @param relTypeUuid - is optional - if not provided, the default value of IS_A_REL is used.
	 * @param time - if null, source concept time is used
	 */
	private TtkRelationshipChronicle addRelationship(TtkConceptChronicle eConcept, UUID targetUuid, UUID relTypeUuid, Long time)
	{
		try
		{
			List<TtkRelationshipChronicle> relationships = eConcept.getRelationships();
			if (relationships == null)
			{
				relationships = new ArrayList<TtkRelationshipChronicle>();
				eConcept.setRelationships(relationships);
			}

			TtkRelationshipChronicle rel = new TtkRelationshipChronicle();
			//this is what {@link RelationshipCAB} does
			rel.setRelGroup(0);
			rel.setPrimordialComponentUuid((UuidT5Generator.get(RelationshipCAB.relSpecNamespace, eConcept.getPrimordialUuid().toString() + relTypeUuid.toString()
					+ targetUuid.toString() + rel.getRelGroup())));
			rel.setC1Uuid(eConcept.getPrimordialUuid());
			rel.setTypeUuid(relTypeUuid == null ? isARelUuid_ : relTypeUuid);
			rel.setC2Uuid(targetUuid);
			rel.setCharacteristicUuid(definingCharacteristicUuid_);
			rel.setRefinabilityUuid(notRefinableUuid);
			
			setRevisionAttributes(rel, null, time == null ? eConcept.getConceptAttributes().getTime() : time);

			relationships.add(rel);
			return rel;
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			throw new RuntimeException("Shouldn't be possible");
		}
	}
	
	/**
	 * See {@link RefexDynamicUsageDescription} class for more details on this format.
	 */
	private void turnConceptIntoDynamicRefexAssemblageConcept(TtkConceptChronicle concept, boolean annotationStyle, String refexDescription,
			List<RefexDynamicColumnInfo> columns) throws PropertyVetoException, NoSuchAlgorithmException, UnsupportedEncodingException {
		turnConceptIntoDynamicRefexAssemblageConcept(concept, annotationStyle, refexDescription, columns.toArray(new RefexDynamicColumnInfo[columns.size()]));
	}
	
	/**
	 * See {@link RefexDynamicUsageDescription} class for more details on this format.
	 */
	private void turnConceptIntoDynamicRefexAssemblageConcept(TtkConceptChronicle concept, boolean annotationStyle, String refexDescription,
			RefexDynamicColumnInfo[] columns) throws PropertyVetoException, NoSuchAlgorithmException, UnsupportedEncodingException
	{
		concept.setAnnotationStyleRefex(annotationStyle);
		//Add the special synonym to establish this as an assemblage concept
		TtkDescriptionChronicle description = addDescription(concept, refexDescription, DescriptionType.DEFINITION, true);
		
		//Annotate the description as the 'special' type that means this concept is suitable for use as an assemblage concept
		addDynamicAnnotation(description, RefexDynamic.REFEX_DYNAMIC_DEFINITION_DESCRIPTION.getUuids()[0], new TtkRefexDynamicData[0]);
		
		if (columns != null)
		{
			for (RefexDynamicColumnInfo col : columns)
			{
				TtkRefexDynamicData[] data = new TtkRefexDynamicData[7];
				data[0] = new TtkRefexDynamicInteger(col.getColumnOrder());
				data[1] = new TtkRefexDynamicUUID(col.getColumnDescriptionConcept());
				data[2] = new TtkRefexDynamicString(col.getColumnDataType().name());
				data[3] = convertPolymorphicDataColumn(col.getDefaultColumnValue(), col.getColumnDataType());
				data[4] = new TtkRefexDynamicBoolean(col.isColumnRequired());
				data[5] = (col.getValidator() == null ? null : new TtkRefexDynamicString(col.getValidator().name()));
				data[6] = (col.getValidatorData() == null ? null : convertPolymorphicDataColumn(col.getValidatorData(), col.getValidatorData().getRefexDataType()));
				addDynamicAnnotation(concept.getConceptAttributes(), RefexDynamic.REFEX_DYNAMIC_DEFINITION.getUuids()[0], data);
			}
		}
	}
	
	private static TtkRefexDynamicData convertPolymorphicDataColumn(RefexDynamicDataBI defaultValue, RefexDynamicDataType columnType) throws PropertyVetoException 
	{
		TtkRefexDynamicData result;
		
		if (defaultValue != null)
		{
			try
			{
				if (RefexDynamicDataType.BOOLEAN == columnType)
				{
					result = new TtkRefexDynamicBoolean((Boolean)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.BYTEARRAY == columnType)
				{
					result = new TtkRefexDynamicByteArray((byte[])defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.DOUBLE == columnType)
				{
					result = new TtkRefexDynamicDouble((Double)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.FLOAT == columnType)
				{
					result = new TtkRefexDynamicFloat((Float)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.INTEGER == columnType)
				{
					result = new TtkRefexDynamicInteger((Integer)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.LONG == columnType)
				{
					result = new TtkRefexDynamicLong((Long)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.NID == columnType)
				{
					result = new TtkRefexDynamicNid((Integer)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.STRING == columnType)
				{
					result = new TtkRefexDynamicString((String)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.UUID == columnType)
				{
					result = new TtkRefexDynamicUUID((UUID)defaultValue.getDataObject());
				}
				else if (RefexDynamicDataType.POLYMORPHIC == columnType)
				{
					throw new RuntimeException("Error in column - if default value is provided, the type cannot be polymorphic");
				}
				else
				{
					throw new RuntimeException("Actually, the implementation is broken.  Ooops.");
				}
			}
			catch (ClassCastException e)
			{
				throw new RuntimeException("Error in column - if default value is provided, the type must be compatible with the the column descriptor type");
			}
		}
		else
		{
			result = null;
		}
		return result;
	}
	
	public static void main(String[] args) throws MojoExecutionException, MojoFailureException
	{
		GenerateMetadataEConcepts gmc = new GenerateMetadataEConcepts(new File("foo.jbin"), new String[] {"org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic"},
				new ConceptSpec[0], false);
		gmc.execute();
	}

}