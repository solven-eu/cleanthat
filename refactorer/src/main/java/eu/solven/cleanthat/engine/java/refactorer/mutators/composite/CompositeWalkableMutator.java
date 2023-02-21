///*
// * Copyright 2023 Benoit Lacelle - SOLVEN
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;
//
//import java.util.List;
//
//import com.google.common.collect.ImmutableList;
//
//import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
//import eu.solven.cleanthat.engine.java.refactorer.meta.IWalkableMutator;
//
///**
// * This mutator make it easy to composite multiple {@link IMutator}s in a single one.
// * 
// * The extended classes should generally implement a constructor taking a JavaVersions as single argument.
// * 
// * @author Benoit Lacelle
// *
// */
//public class CompositeWalkableMutator<AST, R> extends CompositeMutator<IWalkableMutator<AST, R>>
//		implements IWalkableMutator<AST, R> {
//
//	protected CompositeWalkableMutator(List<IWalkableMutator<AST, R>> mutators) {
//		super(ImmutableList.copyOf(mutators));
//	}
//
//	@Override
//	public boolean walkNode(AST pre) {
//		boolean transformed = false;
//
//		for (IWalkableMutator<AST> mutator : mutators) {
//			transformed |= mutator.walkNode(pre);
//		}
//
//		return transformed;
//	}
//
//}
