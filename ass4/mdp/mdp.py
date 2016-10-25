### code for representing/solving an MDP

import random
import numpy
import time
from problem_utils import *

class State :

    def __init__(self) :
        self.utility = 0.0
        self.reward = 0.0
        ### an action maps to a list of probability/state pairs
        self.transitions = {}
        self.actions = []
        self.policy = None
        self.coords = 0
        self.isGoal = False
        self.isWall = False
        self.id = 0

    def computeEU(self, action) :
        return sum([trans[0] * trans[1].utility \
            for trans in self.transitions[action]])

    def selectBestAction(self) :
        best = max([(self.computeEU(a), a) for a in self.actions])
        return best[1]



class Map :
    def __init__(self, stop_crit = 0.01, gamma = 0.8) :
        self.states = {}
        self.stop_crit = stop_crit
        self.gamma = gamma
        self.n_rows = 0
        self.n_cols = 0
    
    class PrintType :
        ACTIONS = 0
        VALUES = 1

    ### you write this method
    def valueIteration(self) :
        ### 1. initialize utilities to 0
        ### 2. repeat value iteration loop until largest change is smaller than
        ###    stop criterion

        #In order to keep the time
        startingTime = time.time()
        
        #First initialize all non-goal state utilities to 0
        for s in self.states.values():
	  if not s.isGoal: 
	    s.utility = 0.0
       
       	#Variables for the while-loop, stop critirion and the iterations counter
	stopCriterionMet = False
        iterations = 0
        
        #This is executed while the stop critirion is not yet met: the difference is still too big
	while not stopCriterionMet:
	    #for each iteration we keep track of the largest difference of all the utilities	
	    largestUtilityDifference = 0.0 
	    
	    #For each non-goal state in the maze we recalculate the utility 
	    for s in self.states.values():
		if not s.isGoal:
			#We recalculate it using the Bellman equation
			#When the difference in utility is bigger than any previous ones it is updated
			if abs(s.utility - self.calculateBellmanUtility(s)) > largestUtilityDifference:
			    largestUtilityDifference = abs(s.utility - self.calculateBellmanUtility(s))
			 
			#The utility of the state is updated  
			s.utility = self.calculateBellmanUtility(s)
	    #We compare the utility difference to the stop critirion to see if another iteration is needed	
	    if largestUtilityDifference < self.stop_crit:
		stopCriterionMet = True
	    
	    #Counter
	    iterations += 1
	
	#First things first, determin the time it took
	runTime = time.time() - startingTime
	#Printing information
	print "Number of iterations used for value iteration: %d" % iterations
	print "Time needed for value iteration: %.6f s" % runTime

    #Helper funtion for the value iteration, to calculate the Bellman equation	
    def calculateBellmanUtility(self, s):
    	#Array we need to store values of which we later pick the maximum
	actionUtilities = []
	
	#Looping over all actions for a state
	for a in s.actions:
	    utilitySum = 0
	    
	    #For each action we calculate the utility based on the possible transitions
	    for trans in s.transitions[a]:
	    	#probability * utility of the state we end up in
		utilitySum += trans[0] * trans[1].utility
	
	    #Add the total utility of one action to the array
	    actionUtilities.append(utilitySum)
	
	#Calculate the final utility using the Bellman equation and return it
        return s.reward + self.gamma * max(actionUtilities)

    ### you write this method
    def policyIteration(self) :
        ### 1. initialize random policy
        ### 2 repeat policy iteration loop until policy is stable
        #In order to keep the time
        startingTime = time.time()
        
        #We initialize by assigning a random policy to each state
        for s in self.states.values():
		if not s.isGoal:
		    s.policy = random.choice(s.actions)
	
	#Variables for the while-loop, stop critirion and the iterations counter	    
	policyStable = False
	iterations = 0
	
	#This is executed while the policies are still changing
	while not policyStable:
	   #We call the calculateUtilitiesLinear function to update the utilities of all states
	   self.calculateUtilitiesLinear()
	   #We call the updatePolicy function to update the policies of all states
	   #The function returns true if non of the policies have changed, otherwise it returns false
	   policyStable = self.updatePolicy()
	   #Counter
	   iterations += 1
	  
	#First things first, we calculate the time it took
	runTime = time.time() - startingTime
	#Printing information
        print "Number of iterations used for policy iteration: %d" % iterations
        print "Time needed for policy iteration: %.6f s" % runTime 
    
    def calculateUtilitiesLinear(self) :
        n_states = len(self.states)
        coeffs = numpy.zeros((n_states, n_states))
        ordinate = numpy.zeros((n_states, 1))
        
        for s in self.states.values() :
            row = s.id
            ordinate[row, 0] = s.reward
            coeffs[row,row] += 1.0
            if not s.isGoal :
                probs = s.transitions[s.policy]
                for p in probs :
                    col = p[1].id
                    coeffs[row,col] += -self.gamma * p[0]
        solution, _, _, _ = numpy.linalg.lstsq(coeffs, ordinate)
        for s in self.states.values() :
            if not s.isGoal :
		s.utility = solution[s.id, 0]
		
    #A helper funtion we wrote which updates the policies for each state	
    def updatePolicy(self):
	policyStable = True
	
	for s in self.states.values():
		if not s.isGoal:
		    #We use the selectBestAction function to find the best action for each state.
		    #Which we set to be the policy for that state
		    bestAction = s.selectBestAction()
		    #Checking if it remained the same since the last iteration
		    if bestAction != s.policy:
			policyStable = False
			
		    s.policy = bestAction
	
	#Return true or false	    
	return policyStable	    
    
    def printActions(self) :
        self.printMaze(self.PrintType.ACTIONS)

    def printValues(self) :
        self.printMaze(self.PrintType.VALUES)
    
    def printMaze(self, print_type) :
        to_print = ":"
        for c in range(self.n_cols) :
            to_print = to_print + "--------:"
        to_print = to_print + '\n'
        for r in range(self.n_rows) :
            to_print = to_print + "|"
            for c in range(self.n_cols) :
                if self.states[(c,r)].isWall :
                    to_print = to_print + "        "
                else:
                    to_print = to_print + ' '
                    if self.states[(c,r)].isGoal :
                        to_print = to_print + \
                        "  {0: d}  ".format(int(self.states[(c,r)].utility))
                    else :
                        if print_type == self.PrintType.VALUES :
                            to_print = to_print + \
                                "{0: .3f}".format(self.states[(c,r)].utility)
                        elif print_type == self.PrintType.ACTIONS :
                            a = self.states[(c,r)].selectBestAction()
                            to_print = to_print + "  "
                            if a == 'left' :
                                to_print = to_print + "<<"
                            elif a == 'right' :
                                to_print = to_print + ">>"
                            elif a == 'up' :
                                to_print = to_print + "/\\"
                            elif a == 'down' :
                                to_print = to_print + "\\/"
                            to_print = to_print + "  "
                    to_print = to_print + ' '
                to_print = to_print + "|"
            to_print = to_print + '\n'
            to_print = to_print + ":"
            for c in range(self.n_cols) :
                to_print = to_print + "--------:"
            to_print = to_print + '\n'
        print to_print

def makeRNProblem() :
    """
    Creates the maze defined in Russell & Norvig. Utilizes functions defined
    in the problem_utils module.
    """


    walls = [(1,1)]
    actions = ['left', 'right','up','down']
    cols = 4
    rows = 3

    def filterState(oldState, newState) :
        if (newState[0] < 0 or newState[1] < 0 or newState[0] > cols - 1  or 
            newState[1] > rows - 1 or newState in walls) :
            return oldState
        else :
            return newState

    m = Map()
    m.n_cols = cols;
    m.n_rows = rows;
    for i in range(m.n_cols) :
        for j in range(m.n_rows) :
            m.states[(i,j)] = State()
            m.states[(i,j)].coords = (i,j)
            m.states[(i,j)].isGoal = False
            m.states[(i,j)].actions = actions
            m.states[(i,j)].id = j * m.n_cols + i
            m.states[(i,j)].reward = -0.04
                    
    m.states[(3,0)].isGoal = True
    m.states[(3,1)].isGoal = True

    m.states[(3,0)].utility = 1.0
    m.states[(3,1)].utility = -1.0

    m.states[(3,0)].reward = 1.0
    m.states[(3,1)].reward = -1.0

    for t in walls :
        m.states[t].isGoal = True
        m.states[t].isWall = True
        m.states[t].reward = 0.0
        m.states[t].utility = 0.0

    for s in m.states.items() :
        for a in actions :
            s[1].transitions[a] = [\
            (0.8, m.states[filterState(s[0], getSuccessor(s[0],a))]),  
            (0.1, m.states[filterState(s[0],getSuccessor(s[0], left(a)))]),
            (0.1, m.states[filterState(s[0], getSuccessor(s[0], right(a)))])]
    return m

def make2DProblem() :
    """
    Creates the larger maze described in the exercise. Utilizes functions 
    defined in the problem_utils module.
    """

    walls = [(1,1), (4,1), (5,1), (6,1),(7,1),(1,2), (7,2), (1,3), (5,3),
             (7,3), (1,4), (5,4), (7,4), (1,5), (5,5), (7,5), (1,6), (5,6), 
             (7,6), (1,7), (5,7), (7,7), (1,8), (3,8), (4,8), (5,8), 
             (7,8), (1,9)]
    actions = ['left', 'right','up','down']

    def filterState(oldState, newState) :
        if (newState[0] < 0 or newState[1] < 0 or newState[0] > 9 or 
            newState[1] > 9 or newState in walls) :
            return oldState
        else :
            return newState

    m = Map()
    m.n_cols = 10;
    m.n_rows = 10;
    for i in range(m.n_cols) :
        for j in range(m.n_rows) :
            m.states[(i,j)] = State()
            m.states[(i,j)].coords = (i,j)
            m.states[(i,j)].isGoal = False
            m.states[(i,j)].actions = actions
            m.states[(i,j)].id = j * 10 + i
            m.states[(i,j)].reward = -0.04
                    
    m.states[(0,9)].isGoal = True
    m.states[(9,9)].isGoal = True
    m.states[(9,0)].isGoal = True

    m.states[(0,9)].utility = 1.0
    m.states[(9,9)].utility = -1.0
    m.states[(9,0)].utility = 1.0

    m.states[(0,9)].reward = 1.0
    m.states[(9,9)].reward = -1.0
    m.states[(9,0)].reward = 1.0

    for t in walls :
        m.states[t].isGoal = True
        m.states[t].isWall = True
        m.states[t].utility = 0.0
        m.states[t].reward = 0.0


    for s in m.states.items() :
        for a in actions :
            s[1].transitions[a] = [\
            (0.7, m.states[filterState(s[0], getSuccessor(s[0],a))]),  
            (0.1, m.states[filterState(s[0], getSuccessor(s[0], opposite(a)))]), 
            (0.1, m.states[filterState(s[0],getSuccessor(s[0], left(a)))]),
            (0.1, m.states[filterState(s[0], getSuccessor(s[0], right(a)))])]

    return m
                   
            
