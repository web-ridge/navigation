﻿using Navigation.Properties;
using System;
using System.CodeDom;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Compilation;
using System.Web.UI;

namespace Navigation
{
	/// <summary>
	/// Allows <see cref="System.Web.UI.Control"/> properties to be set from <see cref="Navigation.NavigationData"/>
	/// using just markup
	/// </summary>
	public class NavigationDataControlBuilderInterceptor : ControlBuilderInterceptor
	{
		private static Regex _NavigationDataBindingExpression = new Regex(@"^\s*\{\s*NavigationData\s+(?<key>[^}]+)\}\s*$");

		/// <summary>
		/// Called before the <see cref="System.Web.UI.ControlBuilder"/> of an element in the markup is initialized
		/// </summary>
		/// <param name="controlBuilder">The control builder which is about to be initialized</param>
		/// <param name="parser">The <see cref="System.Web.UI.TemplateParser"/> which was used to parse the markup</param>
		/// <param name="parentBuilder">The parent control builder</param>
		/// <param name="type">The type of the control that this builder will create</param>
		/// <param name="tagName">The name of the tag to be built</param>
		/// <param name="id">The ID of the element in the markup</param>
		/// <param name="attributes">The list of attributes of the element in the markup</param>
		/// <param name="additionalState">The additional state which can be used to store and retrieve data within
		/// several methods of the <see cref="System.Web.Compilation.ControlBuilderInterceptor"/> class</param>
		public override void PreControlBuilderInit(ControlBuilder controlBuilder, TemplateParser parser, ControlBuilder parentBuilder, Type type, string tagName, string id, IDictionary attributes, IDictionary additionalState)
		{
			if (attributes != null)
			{
				Match navigationDataBindingMatch;
				Dictionary<string, string> navigationDataBindings = new Dictionary<string, string>();
				foreach (DictionaryEntry entry in attributes)
				{
					navigationDataBindingMatch = _NavigationDataBindingExpression.Match((string)entry.Value);
					if (navigationDataBindingMatch.Success)
					{
						navigationDataBindings.Add((string)entry.Key, navigationDataBindingMatch.Groups["key"].Value.Trim());
					}
				}
				if (navigationDataBindings.Count > 0)
				{
					additionalState.Add("___NavigationData", navigationDataBindings);
					foreach (string key in navigationDataBindings.Keys)
						attributes.Remove(key);
				}
			}
		}

		/// <summary>
		/// Called after the <see cref="System.Web.UI.ControlBuilder"/> has completed generating code
		/// </summary>
		/// <param name="controlBuilder">The control builder instance</param>
		/// <param name="codeCompileUnit">A <see cref="System.CodeDom.CodeCompileUnit"/> object that is generated by the compilation</param>
		/// <param name="baseType">The type declaration of the code behind class or derived type</param>
		/// <param name="derivedType">The type declaration of top level markup element</param>
		/// <param name="buildMethod">The method with the necessary code to create the control and set the control's
		/// various properties, events, fields</param>
		/// <param name="dataBindingMethod">The method with code to evaluate data binding expressions within the control</param>
		/// <param name="additionalState">The additional state which can be used to store and retrieve data within
		/// several methods of the <see cref="System.Web.Compilation.ControlBuilderInterceptor"/> class</param>
		public override void OnProcessGeneratedCode(ControlBuilder controlBuilder, CodeCompileUnit codeCompileUnit, CodeTypeDeclaration baseType, CodeTypeDeclaration derivedType, CodeMemberMethod buildMethod, CodeMemberMethod dataBindingMethod, IDictionary additionalState)
		{
			if (buildMethod == null)
				return;
			Dictionary<string, string> navigationDataBindings = additionalState["___NavigationData"] as Dictionary<string, string>;
			if (navigationDataBindings == null)
				return;

			CodeLinePragma linePragma = null;
			foreach (CodeStatement statement in buildMethod.Statements)
			{
				if (statement.LinePragma != null)
					linePragma = statement.LinePragma;
			}
			derivedType.Members.Add(BuildNavigationDataClass(controlBuilder, linePragma, navigationDataBindings));
			CodeObjectCreateExpression navigationDataCreate = new CodeObjectCreateExpression(new CodeTypeReference("@___NavigationData" + controlBuilder.ID), new CodeExpression[] { new CodeVariableReferenceExpression("@__ctrl") });
			CodeDelegateCreateExpression navigationDataDelegate = new CodeDelegateCreateExpression(new CodeTypeReference(typeof(EventHandler)), navigationDataCreate, "Page_SaveStateComplete");
			CodeAttachEventStatement pageAttachEvent = new CodeAttachEventStatement(new CodePropertyReferenceExpression(new CodeThisReferenceExpression(), "Page"), "SaveStateComplete", navigationDataDelegate);
			pageAttachEvent.LinePragma = linePragma;
			buildMethod.Statements.Insert(buildMethod.Statements.Count - 1, pageAttachEvent);
		}

		private static CodeTypeDeclaration BuildNavigationDataClass(ControlBuilder controlBuilder, CodeLinePragma linePragma, Dictionary<string, string> navigationDataBindings)
		{
			CodeTypeDeclaration navigationDataClass = new CodeTypeDeclaration("@___NavigationData" + controlBuilder.ID);
			CodeAttributeDeclaration nonUserCodeAttribute = new CodeAttributeDeclaration(new CodeTypeReference(typeof(DebuggerNonUserCodeAttribute)));
			CodeConstructor constructor = new CodeConstructor();
			constructor.Attributes = MemberAttributes.Public;
			constructor.CustomAttributes.Add(nonUserCodeAttribute);
			constructor.Parameters.Add(new CodeParameterDeclarationExpression(controlBuilder.ControlType, "control"));
			constructor.Statements.Add(new CodeAssignStatement(new CodeFieldReferenceExpression(new CodeThisReferenceExpression(), "_Control"), new CodeVariableReferenceExpression("control")));
			navigationDataClass.Members.Add(constructor);
			CodeMemberField controlField = new CodeMemberField(controlBuilder.ControlType, "_Control");
			navigationDataClass.Members.Add(controlField);
			CodeMemberMethod pageSaveStateListener = new CodeMemberMethod();
			pageSaveStateListener.Name = "Page_SaveStateComplete";
			pageSaveStateListener.Attributes = MemberAttributes.Public;
			pageSaveStateListener.CustomAttributes.Add(nonUserCodeAttribute);
			pageSaveStateListener.Parameters.Add(new CodeParameterDeclarationExpression(new CodeTypeReference(typeof(object)), "sender"));
			pageSaveStateListener.Parameters.Add(new CodeParameterDeclarationExpression(new CodeTypeReference(typeof(EventArgs)), "e"));
			navigationDataClass.Members.Add(pageSaveStateListener);
			BuildNavigationDataStatements(controlBuilder, navigationDataBindings, pageSaveStateListener, linePragma);
			return navigationDataClass;
		}

		private static void BuildNavigationDataStatements(ControlBuilder controlBuilder, Dictionary<string, string> navigationDataBindings, CodeMemberMethod pageSaveStateListener, CodeLinePragma linePragma)
		{
			CodePropertyReferenceExpression navigationData = new CodePropertyReferenceExpression(new CodeTypeReferenceExpression(typeof(StateContext)), "Data");
			CodeAssignStatement controlNavigationDataAssign;
			CodeCastExpression attributeAccessor;
			CodeIndexerExpression navigationDataIndexer;
			CodeExpression[] setAttributeParams;
			CodeExpressionStatement setAttributeInvoke;
			foreach (KeyValuePair<string, string> pair in navigationDataBindings)
			{
				controlNavigationDataAssign = GetNavigationDataAssign(controlBuilder, navigationData, pair);
				if (controlNavigationDataAssign != null)
				{
					controlNavigationDataAssign.LinePragma = linePragma;
					pageSaveStateListener.Statements.Add(controlNavigationDataAssign);
				}
				else
				{
					if (typeof(IAttributeAccessor).IsAssignableFrom(controlBuilder.ControlType))
					{
						attributeAccessor = new CodeCastExpression(typeof(IAttributeAccessor), new CodeFieldReferenceExpression(new CodeThisReferenceExpression(), "_Control"));
						navigationDataIndexer = new CodeIndexerExpression(navigationData, new CodePrimitiveExpression(pair.Value));
						setAttributeParams = new CodeExpression[] { new CodePrimitiveExpression(pair.Key), new CodeCastExpression(typeof(string), navigationDataIndexer) };
						setAttributeInvoke = new CodeExpressionStatement(new CodeMethodInvokeExpression(attributeAccessor, "SetAttribute", setAttributeParams));
						setAttributeInvoke.LinePragma = linePragma;
						pageSaveStateListener.Statements.Add(setAttributeInvoke);
					}
					else
					{
						if (controlBuilder.ControlType.GetProperty(pair.Key, BindingFlags.IgnoreCase | BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public) != null)
							throw new HttpParseException(string.Format(CultureInfo.CurrentCulture, Resources.PropertyReadOnly, pair.Key), null, controlBuilder.PageVirtualPath, null, linePragma.LineNumber);
						throw new HttpParseException(string.Format(CultureInfo.CurrentCulture, Resources.PropertyMissing, controlBuilder.ControlType, pair.Key), null, controlBuilder.PageVirtualPath, null, linePragma.LineNumber);
					}
				}
			}
		}

		private static CodeAssignStatement GetNavigationDataAssign(ControlBuilder controlBuilder, CodePropertyReferenceExpression navigationData, KeyValuePair<string, string> pair)
		{
			PropertyInfo property = controlBuilder.ControlType.GetProperty(pair.Key, BindingFlags.IgnoreCase | BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public);
			if (property != null && property.CanWrite)
			{
				CodePropertyReferenceExpression controlProperty = new CodePropertyReferenceExpression(new CodeFieldReferenceExpression(new CodeThisReferenceExpression(), "_Control"), property.Name);
				CodeIndexerExpression navigationDataIndexer = new CodeIndexerExpression(navigationData, new CodePrimitiveExpression(pair.Value));
				return new CodeAssignStatement(controlProperty, new CodeCastExpression(property.PropertyType, navigationDataIndexer));
			}
			else
			{
				FieldInfo field = controlBuilder.ControlType.GetField(pair.Key, BindingFlags.IgnoreCase | BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public);
				if (field != null)
				{
					CodeFieldReferenceExpression controlField = new CodeFieldReferenceExpression(new CodeFieldReferenceExpression(new CodeThisReferenceExpression(), "_Control"), field.Name);
					CodeIndexerExpression navigationDataIndexer = new CodeIndexerExpression(navigationData, new CodePrimitiveExpression(pair.Value));
					return new CodeAssignStatement(controlField, new CodeCastExpression(field.FieldType, navigationDataIndexer));
				}
			}
			return null;
		}
	}
}
