import mdp
import sys

"""
if len(sys.argv) != 2:
    print "Please enter stop_crit and gamma as arguments"
    exit(1)
"""

repetitions = 100

stop_crit = 0.01
gamma = 0.8

rnpValIt = 0
rnpPolIt = 0
twoDValIt = 0
twoDPolIt = 0

rnpValTime = 0
rnpPolTime = 0
twoDValTime = 0
twoDPolTime = 0

print("gamma: %.2f", gamma)
print("stop_crit: %.3f", stop_crit)
for x in range(repetitions):
    #print "RNP - value"
    m1 = mdp.makeRNProblem(0.001, gamma)
    m1.valueIteration()
    
    if (x == repetitions-1):
	#m1.printValues()
	m1.printActions()
    
    rnpValIt += m1.valIt
    rnpValTime += m1.valRunTime

    #print "======================================="
    #print "RNP - policy"
    m1 = mdp.makeRNProblem(stop_crit, gamma)
    m1.policyIteration()
    
    if (x == repetitions-1):
	#m1.printValues()
	m1.printActions()
    
    rnpPolIt += m1.polIt
    rnpPolTime += m1.polRunTime

    #print "======================================="
    #print "2D - value"
    m2 = mdp.make2DProblem(0.001, gamma)
    m2.valueIteration()
    
    if (x == repetitions-1):
	#m2.printValues()
	m2.printActions()

    twoDValIt += m2.valIt
    twoDValTime += m2.valRunTime

    #print "======================================="
    #print "2D - policy"
    m2 = mdp.make2DProblem(stop_crit, gamma)
    m2.policyIteration()
    
    if (x == repetitions-1):
	#m2.printValues()
	m2.printActions()
    
    twoDPolIt += m2.polIt
    twoDPolTime += m2.polRunTime

print "Average RN val. it.: %d" % round((float)(rnpValIt/repetitions))
print "Average 2D val. it.: %d" % round((float)(twoDValIt/repetitions))
print "Average RN pol. it.: %d" % round((float)(rnpPolIt/repetitions))
print "Average 2D pol. it.: %d" % round((float)(twoDPolIt/repetitions))

print "Average RN val. time: %.4f" % (1000*rnpValTime/repetitions)
print "Average 2D val. time: %.4f" % (1000*twoDValTime/repetitions)
print "Average RN pol. time: %.4f" % (1000*rnpPolTime/repetitions)
print "Average 2D pol. time: %.4f" % (1000*twoDPolTime/repetitions)